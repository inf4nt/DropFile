
package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfiledaemon.crypto.CryptoConstants;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore.SessionIn;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.service.AccessKeyService;
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
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerHandshakeFailureIntegrationTest {

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
    private AccessKeyService accessKeyService;

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
    void handshakeRejectsMissingAccessKey() throws Exception {
        String secret = UUID.randomUUID().toString();
        String accessKeyId = accessKeyService.getId(secret);

        PreparedRequest prepared = createRequest(accessKeyId, secret, false);

        HttpResponse<byte[]> response = post(prepared.request());

        assertThat("missing access key must be rejected", response.statusCode(), is(404));
        assertThat(accessKeyStore.get(accessKeyId).isEmpty(), is(true));

        assertNoHandshakeState(prepared.clientFingerprint());
    }

    @Test
    void handshakeRejectsExpiredAccessKey() throws Exception {
        String secret = UUID.randomUUID().toString();
        String accessKeyId = accessKeyService.getId(secret);

        accessKeyStore.save(
                accessKeyId,
                new AccessKey(
                        secret,
                        Duration.ofMinutes(1),
                        Instant.now().minus(Duration.ofMinutes(2))
                )
        );

        PreparedRequest prepared = createRequest(accessKeyId, secret, false);

        HttpResponse<byte[]> response = post(prepared.request());

        assertThat("expired access key must be rejected", response.statusCode(), is(404));

        // AccessKey is consumed before its expiration is checked.
        assertThat(accessKeyStore.get(accessKeyId).isEmpty(), is(true));

        assertNoHandshakeState(prepared.clientFingerprint());
    }

    @Test
    void handshakeRejectsInvalidRequestSignature() throws Exception {
        String secret = UUID.randomUUID().toString();
        String accessKeyId = accessKeyService.getId(secret);

        accessKeyStore.save(
                accessKeyId,
                new AccessKey(secret, Duration.ofMinutes(5), Instant.now())
        );

        PreparedRequest prepared = createRequest(accessKeyId, secret, true);

        HttpResponse<byte[]> response = post(prepared.request());

        assertThat("invalid signature must be rejected", response.statusCode(), is(404));

        // AccessKey is consumed before signature verification.
        assertThat(
                "invalidly signed request must consume access key",
                accessKeyStore.get(accessKeyId).isEmpty(),
                is(true)
        );

        assertNoHandshakeState(prepared.clientFingerprint());
    }

    @Test
    void handshakeRejectsReplayedRequestEvenWithAnotherAccessKey() throws Exception {
        String firstSecret = UUID.randomUUID().toString();
        String firstAccessKeyId = accessKeyService.getId(firstSecret);

        accessKeyStore.save(
                firstAccessKeyId,
                new AccessKey(firstSecret, Duration.ofMinutes(5), Instant.now())
        );

        String secondSecret = UUID.randomUUID().toString();
        String secondAccessKeyId = accessKeyService.getId(secondSecret);

        accessKeyStore.save(
                secondAccessKeyId,
                new AccessKey(secondSecret, Duration.ofMinutes(5), Instant.now())
        );

        KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();

        HandshakeRequestDTO.Payload payload = new HandshakeRequestDTO.Payload(
                UUID.randomUUID(),
                CommonUtils.nonce16(),
                rsaKeyPair.getPublic().getEncoded(),
                CryptoECDH.generateKeyPair().getPublic().getEncoded(),
                Instant.now().toEpochMilli()
        );

        PreparedRequest firstRequest = createRequest(
                firstAccessKeyId,
                firstSecret,
                payload,
                rsaKeyPair
        );

        PreparedRequest replayRequest = createRequest(
                secondAccessKeyId,
                secondSecret,
                payload,
                rsaKeyPair
        );

        assertThat(
                "both requests must have the same client fingerprint",
                replayRequest.clientFingerprint(),
                is(firstRequest.clientFingerprint())
        );

        HttpResponse<byte[]> firstResponse = post(firstRequest.request());
        HttpResponse<byte[]> replayResponse = post(replayRequest.request());

        assertThat("first handshake must succeed", firstResponse.statusCode(), is(200));
        assertThat("replayed handshake must be rejected", replayResponse.statusCode(), is(404));

        // Both keys are consumed: the first by the successful handshake,
        // the second by the replay attempt before replay validation.
        assertThat(accessKeyStore.get(firstAccessKeyId).isEmpty(), is(true));
        assertThat(accessKeyStore.get(secondAccessKeyId).isEmpty(), is(true));

        // The rejected replay must not replace the state created by the first handshake.
        HandshakeTrustedInStore.TrustedIn trusted = handshakeTrustedInStore
                .getRequired(firstRequest.clientFingerprint())
                .getValue();

        SessionIn session = handshakeSessionInStore
                .getRequired(firstRequest.clientFingerprint())
                .getValue();

        assertThat(trusted.sessionId(), is(payload.requestId()));
        assertThat(session.sessionId(), is(payload.requestId()));
    }

    private PreparedRequest createRequest(
            String accessKeyId,
            String secret,
            boolean invalidateSignature
    ) throws Exception {
        KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();

        HandshakeRequestDTO.Payload payload = new HandshakeRequestDTO.Payload(
                UUID.randomUUID(),
                CommonUtils.nonce16(),
                rsaKeyPair.getPublic().getEncoded(),
                CryptoECDH.generateKeyPair().getPublic().getEncoded(),
                Instant.now().toEpochMilli()
        );

        return createRequest(accessKeyId, secret, payload, rsaKeyPair, invalidateSignature);
    }

    private PreparedRequest createRequest(
            String accessKeyId,
            String secret,
            HandshakeRequestDTO.Payload payload,
            KeyPair rsaKeyPair
    ) throws Exception {
        return createRequest(accessKeyId, secret, payload, rsaKeyPair, false);
    }

    private PreparedRequest createRequest(
            String accessKeyId,
            String secret,
            HandshakeRequestDTO.Payload payload,
            KeyPair rsaKeyPair,
            boolean invalidateSignature
    ) throws Exception {
        byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
        byte[] aad = accessKeyId.getBytes(StandardCharsets.UTF_8);

        SecretKey handshakeKey = cryptoTunnel.deriveSecretKey(
                secret.getBytes(StandardCharsets.UTF_8),
                CryptoConstants.HANDSHAKE_SECRET_CLIENT_INFO
        );

        var envelope = cryptoTunnel.encrypt(payloadBytes, aad, handshakeKey);

        byte[] signature = CryptoRSA.sign(payloadBytes, rsaKeyPair.getPrivate());

        if (invalidateSignature) {
            signature = signature.clone();
            signature[0] ^= 1;
        }

        HandshakeRequestDTO request = new HandshakeRequestDTO(
                accessKeyId,
                envelope.payload(),
                envelope.nonce(),
                signature
        );

        String clientFingerprint = CommonUtils.getFingerprint(
                rsaKeyPair.getPublic().getEncoded()
        );

        return new PreparedRequest(request, clientFingerprint);
    }

    private void assertNoHandshakeState(String clientFingerprint) {
        assertThat(
                "trusted state must not be created",
                handshakeTrustedInStore.get(clientFingerprint).isEmpty(),
                is(true)
        );

        assertThat(
                "session state must not be created",
                handshakeSessionInStore.get(clientFingerprint).isEmpty(),
                is(true)
        );
    }

    private HttpResponse<byte[]> post(HandshakeRequestDTO requestDTO) throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:%d%s".formatted(
                        port,
                        ServerHandshakeRestController.HANDSHAKE_ENDPOINT
                )))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(requestDTO)
                ))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private record PreparedRequest(
            HandshakeRequestDTO request,
            String clientFingerprint
    ) {
    }
}
