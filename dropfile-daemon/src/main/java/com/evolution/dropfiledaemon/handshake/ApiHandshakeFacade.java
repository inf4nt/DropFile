package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.evolution.dropfiledaemon.util.AccessKeyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiHandshakeFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final HandshakeClient handshakeClient;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public synchronized ApiHandshakeStatusResponseDTO handshake(ApiHandshakeRequestDTO requestDTO) {
        URI addressURI = CommonUtils.toURI(requestDTO.address());
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> existingAddressURI = applicationConfigStore.getHandshakeContextStore()
                .trustedOutStore()
                .getByAddressURI(addressURI)
                .orElse(null);
        if (existingAddressURI != null) {
            throw new RuntimeException(String.format(
                    "Unable to process handshake request. Duplicate address URI %s fingerprint %s. Try to perform disconnect",
                    addressURI, existingAddressURI.getKey()
            ));
        }

        KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();
        KeyPair dhKeyPair = CryptoECDH.generateKeyPair();

        HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                rsaKeyPair.getPublic().getEncoded(),
                dhKeyPair.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] requestPayloadByteArray = objectMapper.writeValueAsBytes(requestPayload);

        String secret = new String(CommonUtils.decodeBase64(requestDTO.key()));
        SecretKey secretKey = cryptoTunnel.secretKey(secret.getBytes());
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                requestPayloadByteArray,
                secretKey
        );

        byte[] signature = CryptoRSA.sign(
                requestPayloadByteArray,
                rsaKeyPair.getPrivate()
        );

        String requestId = AccessKeyUtils.getId(secret);
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                requestId,
                secureEnvelope.payload(),
                secureEnvelope.nonce(),
                signature
        );

        HandshakeResponseDTO handshakeResponseDTO = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                handshakeResponseDTO.payload(),
                handshakeResponseDTO.nonce(),
                secretKey
        );

        HandshakeResponseDTO.Payload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.Payload.class
        );
        HandshakeUtils.validateHandshakeLiveTimeout(responsePayload.timestamp());

        CryptoRSA.verify(
                decryptResponsePayload,
                handshakeResponseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );

        String remoteFingerprint = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());

        applicationConfigStore.getHandshakeContextStore()
                .trustedOutStore()
                .save(
                        remoteFingerprint,
                        new HandshakeTrustedOutStore.TrustedOut(
                                addressURI,
                                rsaKeyPair.getPublic().getEncoded(),
                                rsaKeyPair.getPrivate().getEncoded(),
                                responsePayload.publicKeyRSA(),
                                Instant.now()
                        )
                );
        applicationConfigStore.getHandshakeContextStore()
                .sessionStore()
                .save(
                        remoteFingerprint,
                        new HandshakeSessionStore.SessionValue(
                                dhKeyPair.getPublic().getEncoded(),
                                dhKeyPair.getPrivate().getEncoded(),
                                responsePayload.publicKeyDH(),
                                Instant.now()
                        )
                );
        return new ApiHandshakeStatusResponseDTO(
                remoteFingerprint,
                addressURI.toString()
        );
    }

    @SneakyThrows
    public synchronized ApiHandshakeStatusResponseDTO handshakeReconnect(ApiHandshakeReconnectRequestDTO requestDTO) {
        URI addressURI = CommonUtils.toURI(requestDTO.address());
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> trustedOutEntry = applicationConfigStore.getHandshakeContextStore().trustedOutStore()
                .getRequiredByAddressURI(addressURI);

        KeyPair keyPairDH = CryptoECDH.generateKeyPair();
        HandshakeSessionDTO.SessionPayload sessionPayloadRequest = new HandshakeSessionDTO.SessionPayload(
                keyPairDH.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] sessionPayloadRequestBytes = objectMapper.writeValueAsBytes(sessionPayloadRequest);
        byte[] signature = CryptoRSA.sign(sessionPayloadRequestBytes, CryptoRSA.getPrivateKey(trustedOutEntry.getValue().privateRSA()));
        HandshakeSessionDTO.Session sessionRequest = new HandshakeSessionDTO.Session(
                CommonUtils.getFingerprint(trustedOutEntry.getValue().publicRSA()),
                sessionPayloadRequestBytes,
                signature
        );
        HandshakeSessionDTO.Session sessionResponse = handshakeClient.handshakeSession(addressURI, sessionRequest);
        CryptoRSA.verify(
                sessionResponse.payload(),
                sessionResponse.signature(),
                CryptoRSA.getPublicKey(trustedOutEntry.getValue().remoteRSA())
        );
        HandshakeSessionDTO.SessionPayload sessionPayload = objectMapper.readValue(sessionResponse.payload(), HandshakeSessionDTO.SessionPayload.class);

        HandshakeUtils.validateHandshakeLiveTimeout(sessionPayload.timestamp());

        String remoteFingerprint = sessionResponse.fingerprint();
        HandshakeUtils.matchFingerprint(remoteFingerprint, CryptoRSA.getPublicKey(trustedOutEntry.getValue().remoteRSA()));

        applicationConfigStore.getHandshakeContextStore().sessionStore()
                .save(
                        remoteFingerprint,
                        new HandshakeSessionStore.SessionValue(
                                keyPairDH.getPublic().getEncoded(),
                                keyPairDH.getPrivate().getEncoded(),
                                sessionPayload.publicKey(),
                                Instant.now()
                        )
                );

        return new ApiHandshakeStatusResponseDTO(
                remoteFingerprint,
                addressURI.toString()
        );
    }

    public synchronized ApiHandshakeStatusResponseDTO handshakeStatus() {
        String currentConnectionFingerprint = applicationConfigStore.getHandshakeContextStore().sessionStore()
                .getRequiredLatestUpdated().getKey();
        URI addressURI = applicationConfigStore.getHandshakeContextStore().trustedOutStore()
                .getRequired(currentConnectionFingerprint)
                .getValue()
                .addressURI();
        return handshakeReconnect(new ApiHandshakeReconnectRequestDTO(addressURI.toString()));
    }

    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        Map<String, HandshakeTrustedOutStore.TrustedOut> trusts = applicationConfigStore.getHandshakeContextStore().trustedOutStore().getAll();
        Map<String, HandshakeSessionStore.SessionValue> sessions = applicationConfigStore.getHandshakeContextStore().sessionStore().getAll();
        return HandshakeUtils.mapToHandshakeApiTrustOutResponseDTOList(trusts, sessions);
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        Map<String, HandshakeTrustedInStore.TrustedIn> trusts = applicationConfigStore.getHandshakeContextStore().trustedInStore().getAll();
        Map<String, HandshakeSessionStore.SessionValue> sessions = applicationConfigStore.getHandshakeContextStore().sessionStore().getAll();
        return HandshakeUtils.mapToHandshakeApiTrustInResponseDTOList(trusts, sessions);
    }

    public HandshakeApiTrustOutResponseDTO getLatestTrustOut() {
        Map.Entry<String, HandshakeSessionStore.SessionValue> sessionEntry = applicationConfigStore.getHandshakeContextStore().sessionStore()
                .getRequiredLatestUpdated();
        HandshakeTrustedOutStore.TrustedOut trustedOut = applicationConfigStore.getHandshakeContextStore().trustedOutStore().getRequired(sessionEntry.getKey())
                .getValue();
        return HandshakeUtils.mapToHandshakeApiTrustOutResponseDTO(sessionEntry.getKey(), trustedOut, sessionEntry.getValue());
    }
}
