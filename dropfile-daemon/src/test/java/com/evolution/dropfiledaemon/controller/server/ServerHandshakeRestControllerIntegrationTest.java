
package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfiledaemon.crypto.CryptoConstants;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeReadinessRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeReadinessResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore.SessionIn;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedInStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerHandshakeRestControllerIntegrationTest {

    @TempDir
    private static Path tempDirectory;

    @DynamicPropertySource
    static void configureTemporaryDirectories(DynamicPropertyRegistry registry) {
        registry.add("dropfile.daemon.application-home.directory", () -> tempDirectory.toString());
        registry.add("dropfile.daemon.security.replay.max-future-drift-timeout", () -> "0");
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private CryptoTunnel cryptoTunnel;

    @Autowired
    private AccessKeyStore accessKeyStore;

    @Autowired
    private HandshakeSessionInStore handshakeSessionInStore;

    @Autowired
    private HandshakeTrustedInStore handshakeTrustedInStore;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void clearStores() {
        accessKeyStore.removeAll();
        handshakeSessionInStore.removeAll();
        handshakeTrustedInStore.removeAll();
    }

    @Test
    void handshakeEndpointCompletesHandshakeSuccessfully() throws Exception {
        String accessKeyId = UUID.randomUUID().toString();
        String accessKey = UUID.randomUUID().toString();
        Instant now = Instant.now();

        accessKeyStore.save(accessKeyId, new AccessKey(accessKey, Duration.ofMinutes(5), now));

        KeyPair clientRsaKeyPair = CryptoRSA.generateKeyPair();
        KeyPair clientDhKeyPair = CryptoECDH.generateKeyPair();
        UUID requestId = UUID.randomUUID();
        byte[] clientSalt = CommonUtils.nonce16();

        HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                requestId,
                clientSalt,
                clientRsaKeyPair.getPublic().getEncoded(),
                clientDhKeyPair.getPublic().getEncoded(),
                Instant.now().toEpochMilli()
        );

        byte[] requestPayloadBytes = objectMapper.writeValueAsBytes(requestPayload);
        byte[] aad = accessKeyId.getBytes(StandardCharsets.UTF_8);

        SecretKey clientHandshakeKey = cryptoTunnel.deriveSecretKey(
                accessKey.getBytes(StandardCharsets.UTF_8),
                CryptoConstants.HANDSHAKE_SECRET_CLIENT_INFO
        );

        var requestEnvelope = cryptoTunnel.encrypt(requestPayloadBytes, aad, clientHandshakeKey);

        HandshakeRequestDTO requestDTO = new HandshakeRequestDTO(
                accessKeyId,
                requestEnvelope.payload(),
                requestEnvelope.nonce(),
                CryptoRSA.sign(requestPayloadBytes, clientRsaKeyPair.getPrivate())
        );

        HttpResponse<byte[]> result = post(ServerHandshakeRestController.HANDSHAKE_ENDPOINT, requestDTO);

        assertThat(result.statusCode(), is(200));

        HandshakeResponseDTO responseDTO = objectMapper.readValue(
                result.body(),
                HandshakeResponseDTO.class
        );

        SecretKey serverHandshakeKey = cryptoTunnel.deriveSecretKey(
                accessKey.getBytes(StandardCharsets.UTF_8),
                CryptoConstants.HANDSHAKE_SECRET_SERVER_INFO
        );

        byte[] responsePayloadBytes = cryptoTunnel.decrypt(
                responseDTO.payload(),
                responseDTO.nonce(),
                aad,
                serverHandshakeKey
        );

        HandshakeResponseDTO.Payload responsePayload = objectMapper.readValue(
                responsePayloadBytes,
                HandshakeResponseDTO.Payload.class
        );

        assertThat(responsePayload.requestId(), is(requestId));
        assertThat(responsePayload.serverSalt().length, is(16));

        CryptoRSA.verify(
                responsePayloadBytes,
                responseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );

        assertThat("access key must be consumed", accessKeyStore.get(accessKeyId).isPresent(), is(false));

        String remoteFingerprint = CommonUtils.getFingerprint(clientRsaKeyPair.getPublic().getEncoded());

        HandshakeTrustedInStore.TrustedIn trustedIn =
                handshakeTrustedInStore.getRequired(remoteFingerprint).getValue();

        assertThat(
                Arrays.equals(
                        clientRsaKeyPair.getPublic().getEncoded(),
                        trustedIn.handshake().remoteRSA()
                ),
                is(true)
        );
        assertThat(trustedIn.sessionId(), is(requestId));

        byte[] sessionRawKey = CryptoECDH.getSecretKey(
                clientDhKeyPair.getPrivate(),
                CryptoECDH.getPublicKey(responsePayload.publicKeyDH())
        );

        byte[] salt = CommonUtils.salt(clientSalt, responsePayload.serverSalt());

        byte[] expectedClientTunnelKey = cryptoTunnel.deriveSecretKey(
                sessionRawKey,
                CryptoConstants.TUNNEL_SECRET_CLIENT_INFO,
                salt
        ).getEncoded();

        byte[] expectedServerTunnelKey = cryptoTunnel.deriveSecretKey(
                sessionRawKey,
                CryptoConstants.TUNNEL_SECRET_SERVER_INFO,
                salt
        ).getEncoded();

        SessionIn session = handshakeSessionInStore.getRequired(remoteFingerprint).getValue();

        assertThat(Arrays.equals(expectedClientTunnelKey, session.clientKey()), is(true));
        assertThat(Arrays.equals(expectedServerTunnelKey, session.serverKey()), is(true));
        assertThat(session.sessionId(), is(requestId));
    }

    @Test
    void handshakeSessionEndpointCompletesSessionHandshakeSuccessfully() throws Exception {
        EstablishedHandshake handshake = establishHandshake();

        KeyPair clientDhKeyPair = CryptoECDH.generateKeyPair();
        UUID requestId = UUID.randomUUID();
        byte[] clientSalt = CommonUtils.nonce16();

        HandshakeSessionRequestDTO.Payload requestPayload = new HandshakeSessionRequestDTO.Payload(
                requestId,
                clientSalt,
                clientDhKeyPair.getPublic().getEncoded(),
                Instant.now().toEpochMilli()
        );

        byte[] requestPayloadBytes = objectMapper.writeValueAsBytes(requestPayload);

        HandshakeSessionRequestDTO requestDTO = new HandshakeSessionRequestDTO(
                handshake.clientFingerprint(),
                requestPayloadBytes,
                CryptoRSA.sign(requestPayloadBytes, handshake.clientRsaKeyPair().getPrivate())
        );

        HttpResponse<byte[]> result = post(
                ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT,
                requestDTO
        );

        assertThat(result.statusCode(), is(200));

        HandshakeSessionResponseDTO responseDTO = objectMapper.readValue(
                result.body(),
                HandshakeSessionResponseDTO.class
        );

        assertThat(responseDTO.fingerprint(), is(handshake.serverFingerprint()));

        HandshakeSessionResponseDTO.Payload responsePayload = objectMapper.readValue(
                responseDTO.payload(),
                HandshakeSessionResponseDTO.Payload.class
        );

        assertThat(responsePayload.requestId(), is(requestId));
        assertThat(responsePayload.serverSalt().length, is(16));

        CryptoRSA.verify(
                responseDTO.payload(),
                responseDTO.signature(),
                CryptoRSA.getPublicKey(handshake.serverHandshakePayload().publicKeyRSA())
        );

        byte[] sessionRawKey = CryptoECDH.getSecretKey(
                clientDhKeyPair.getPrivate(),
                CryptoECDH.getPublicKey(responsePayload.publicKeyDH())
        );

        byte[] salt = CommonUtils.salt(clientSalt, responsePayload.serverSalt());

        byte[] expectedClientKey = cryptoTunnel.deriveSecretKey(
                sessionRawKey,
                CryptoConstants.TUNNEL_SECRET_CLIENT_INFO,
                salt
        ).getEncoded();

        byte[] expectedServerKey = cryptoTunnel.deriveSecretKey(
                sessionRawKey,
                CryptoConstants.TUNNEL_SECRET_SERVER_INFO,
                salt
        ).getEncoded();

        SessionIn session = handshakeSessionInStore
                .getRequired(handshake.clientFingerprint())
                .getValue();

        assertThat(Arrays.equals(expectedClientKey, session.clientKey()), is(true));
        assertThat(Arrays.equals(expectedServerKey, session.serverKey()), is(true));
        assertThat(session.sessionId(), is(requestId));

        HandshakeTrustedInStore.TrustedIn trusted = handshakeTrustedInStore
                .getRequired(handshake.clientFingerprint())
                .getValue();

        assertThat(trusted.sessionId(), is(requestId));
    }

    @Test
    void handshakeReadinessEndpointCompletesReadinessHandshakeSuccessfully() throws Exception {
        EstablishedHandshake handshake = establishHandshake();

        UUID requestId = UUID.randomUUID();

        HandshakeReadinessRequestDTO.Payload requestPayload = new HandshakeReadinessRequestDTO.Payload(
                requestId,
                Instant.now().toEpochMilli()
        );

        byte[] requestPayloadBytes = objectMapper.writeValueAsBytes(requestPayload);

        HandshakeReadinessRequestDTO requestDTO = new HandshakeReadinessRequestDTO(
                handshake.clientFingerprint(),
                requestPayloadBytes,
                CryptoRSA.sign(requestPayloadBytes, handshake.clientRsaKeyPair().getPrivate())
        );

        HttpResponse<byte[]> result = post(
                ServerHandshakeRestController.HANDSHAKE_READINESS_ENDPOINT,
                requestDTO
        );

        assertThat(result.statusCode(), is(200));

        HandshakeReadinessResponseDTO responseDTO = objectMapper.readValue(
                result.body(),
                HandshakeReadinessResponseDTO.class
        );

        assertThat(responseDTO.fingerprint(), is(handshake.serverFingerprint()));

        HandshakeReadinessResponseDTO.Payload responsePayload = objectMapper.readValue(
                responseDTO.payload(),
                HandshakeReadinessResponseDTO.Payload.class
        );

        assertThat(responsePayload.requestId(), is(requestId));

        CryptoRSA.verify(
                responseDTO.payload(),
                responseDTO.signature(),
                CryptoRSA.getPublicKey(handshake.serverHandshakePayload().publicKeyRSA())
        );
    }

    private EstablishedHandshake establishHandshake() throws Exception {
        String accessKeyId = UUID.randomUUID().toString();
        String accessKey = UUID.randomUUID().toString();

        accessKeyStore.save(
                accessKeyId,
                new AccessKey(accessKey, Duration.ofMinutes(5), Instant.now())
        );

        KeyPair clientRsaKeyPair = CryptoRSA.generateKeyPair();
        KeyPair clientDhKeyPair = CryptoECDH.generateKeyPair();
        UUID requestId = UUID.randomUUID();
        byte[] clientSalt = CommonUtils.nonce16();

        HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                requestId,
                clientSalt,
                clientRsaKeyPair.getPublic().getEncoded(),
                clientDhKeyPair.getPublic().getEncoded(),
                Instant.now().toEpochMilli()
        );

        byte[] requestPayloadBytes = objectMapper.writeValueAsBytes(requestPayload);
        byte[] aad = accessKeyId.getBytes(StandardCharsets.UTF_8);

        SecretKey clientHandshakeKey = cryptoTunnel.deriveSecretKey(
                accessKey.getBytes(StandardCharsets.UTF_8),
                CryptoConstants.HANDSHAKE_SECRET_CLIENT_INFO
        );

        var requestEnvelope = cryptoTunnel.encrypt(requestPayloadBytes, aad, clientHandshakeKey);

        HandshakeRequestDTO requestDTO = new HandshakeRequestDTO(
                accessKeyId,
                requestEnvelope.payload(),
                requestEnvelope.nonce(),
                CryptoRSA.sign(requestPayloadBytes, clientRsaKeyPair.getPrivate())
        );

        HttpResponse<byte[]> result = post(ServerHandshakeRestController.HANDSHAKE_ENDPOINT, requestDTO);

        assertThat(result.statusCode(), is(200));

        HandshakeResponseDTO responseDTO = objectMapper.readValue(
                result.body(),
                HandshakeResponseDTO.class
        );

        SecretKey serverHandshakeKey = cryptoTunnel.deriveSecretKey(
                accessKey.getBytes(StandardCharsets.UTF_8),
                CryptoConstants.HANDSHAKE_SECRET_SERVER_INFO
        );

        byte[] responsePayloadBytes = cryptoTunnel.decrypt(
                responseDTO.payload(),
                responseDTO.nonce(),
                aad,
                serverHandshakeKey
        );

        HandshakeResponseDTO.Payload responsePayload = objectMapper.readValue(
                responsePayloadBytes,
                HandshakeResponseDTO.Payload.class
        );

        assertThat(responsePayload.requestId(), is(requestId));
        assertThat(responsePayload.serverSalt().length, is(16));

        CryptoRSA.verify(
                responsePayloadBytes,
                responseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );

        assertThat(
                "access key must be consumed",
                accessKeyStore.get(accessKeyId).isPresent(),
                is(false)
        );

        String clientFingerprint = CommonUtils.getFingerprint(clientRsaKeyPair.getPublic().getEncoded());
        String serverFingerprint = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());

        HandshakeTrustedInStore.TrustedIn trusted = handshakeTrustedInStore
                .getRequired(clientFingerprint)
                .getValue();

        SessionIn session = handshakeSessionInStore
                .getRequired(clientFingerprint)
                .getValue();

        assertThat(trusted.sessionId(), is(requestId));
        assertThat(session.sessionId(), is(requestId));

        return new EstablishedHandshake(
                clientRsaKeyPair,
                clientFingerprint,
                serverFingerprint,
                responsePayload
        );
    }

    private HttpResponse<byte[]> post(String endpoint, Object requestBody) throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:%d%s".formatted(port, endpoint)))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(requestBody)))
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    private record EstablishedHandshake(
            KeyPair clientRsaKeyPair,
            String clientFingerprint,
            String serverFingerprint,
            HandshakeResponseDTO.Payload serverHandshakePayload
    ) {
    }
}
