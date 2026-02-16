package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class HandshakeFacade {

    private static final int MAX_HANDSHAKE_PAYLOAD_LIVE_TIMEOUT = 30_000;

    private final ApplicationConfigStore applicationConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public synchronized HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();
        Map.Entry<String, AccessKey> accessKey = applicationConfigStore.getAccessKeyStore()
                .getRequired(accessKeyId);
        applicationConfigStore.getAccessKeyStore().remove(requestDTO.id());

        SecretKey secretKey = cryptoTunnel.secretKey(accessKey.getValue().key().getBytes());
        byte[] decryptMessage = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        HandshakeRequestDTO.Payload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.Payload.class);
        CryptoRSA.verify(
                decryptMessage,
                requestDTO.signature(),
                CryptoRSA.getPublicKey(requestPayload.publicKeyRSA())
        );
        validatePayloadLiveTimeout(requestPayload.timestamp());

        KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();
        KeyPair dhKeyPair = CryptoECDH.generateKeyPair();

        HandshakeResponseDTO.Payload responsePayload = new HandshakeResponseDTO.Payload(
                rsaKeyPair.getPublic().getEncoded(),
                dhKeyPair.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] responsePayloadByteArray = objectMapper.writeValueAsBytes(responsePayload);

        byte[] signature = CryptoRSA.sign(
                responsePayloadByteArray,
                CryptoRSA.getPrivateKey(rsaKeyPair.getPrivate().getEncoded())
        );
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                responsePayloadByteArray,
                secretKey
        );
        HandshakeResponseDTO handshakeResponseDTO = new HandshakeResponseDTO(
                secureEnvelope.payload(),
                secureEnvelope.nonce(),
                signature
        );

        byte[] publicKeyRSA = requestPayload.publicKeyRSA();
        String fingerprint = CommonUtils.getFingerprint(publicKeyRSA);
        applicationConfigStore.getHandshakeContextStore().trustedInStore()
                .save(
                        fingerprint,
                        new HandshakeTrustedInStore.TrustedIn(
                                rsaKeyPair.getPublic().getEncoded(),
                                rsaKeyPair.getPrivate().getEncoded(),
                                publicKeyRSA,
                                Instant.now()
                        )
                );

        byte[] publicKeyDH = requestPayload.publicKeyDH();
        applicationConfigStore.getHandshakeContextStore().sessionStore()
                .save(
                        fingerprint,
                        new HandshakeSessionStore.SessionValue(
                                dhKeyPair.getPublic().getEncoded(),
                                dhKeyPair.getPrivate().getEncoded(),
                                publicKeyDH,
                                Instant.now()
                        )
                );
        return handshakeResponseDTO;
    }

    @SneakyThrows
    public synchronized HandshakeSessionDTO.Session handshakeSession(HandshakeSessionDTO.Session sessionDTO) {
        HandshakeTrustedInStore.TrustedIn trustedIn = applicationConfigStore.getHandshakeContextStore()
                .trustedInStore()
                .getRequired(sessionDTO.fingerprint())
                .getValue();
        String fingerprint = CommonUtils.getFingerprint(trustedIn.remoteRSA());
        if (!sessionDTO.fingerprint().equals(fingerprint)) {
            throw new RuntimeException("Fingerprint mismatch");
        }

        byte[] sessionPayloadDTOBytes = sessionDTO.payload();

        CryptoRSA.verify(
                sessionPayloadDTOBytes,
                sessionDTO.signature(),
                CryptoRSA.getPublicKey(trustedIn.remoteRSA())
        );

        HandshakeSessionDTO.SessionPayload sessionPayload = objectMapper.readValue(
                sessionPayloadDTOBytes, HandshakeSessionDTO.SessionPayload.class
        );
        validatePayloadLiveTimeout(sessionPayload.timestamp());

        KeyPair keyPairDH = CryptoECDH.generateKeyPair();

        HandshakeSessionDTO.SessionPayload sessionPayloadResponse = new HandshakeSessionDTO.SessionPayload(
                keyPairDH.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] sessionPayloadResponseBytes = objectMapper.writeValueAsBytes(sessionPayloadResponse);
        byte[] signature = CryptoRSA.sign(sessionPayloadResponseBytes, CryptoRSA.getPrivateKey(trustedIn.privateRSA()));
        HandshakeSessionDTO.Session sessionResponse = new HandshakeSessionDTO.Session(
                CommonUtils.getFingerprint(trustedIn.publicRSA()),
                sessionPayloadResponseBytes,
                signature
        );

        applicationConfigStore.getHandshakeContextStore()
                .sessionStore()
                .save(
                        fingerprint,
                        new HandshakeSessionStore.SessionValue(
                                keyPairDH.getPublic().getEncoded(),
                                keyPairDH.getPrivate().getEncoded(),
                                sessionPayload.publicKey(),
                                Instant.now()
                        )
                );

        return sessionResponse;
    }

    private void validatePayloadLiveTimeout(long timestamp) {
        if (timestamp <= 0) {
            throw new RuntimeException("Invalid timestamp");
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_HANDSHAKE_PAYLOAD_LIVE_TIMEOUT) {
            throw new RuntimeException("Timed out");
        }
    }
}
