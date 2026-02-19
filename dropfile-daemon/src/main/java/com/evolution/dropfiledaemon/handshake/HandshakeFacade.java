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
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@Component
public class HandshakeFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public synchronized HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();
        AccessKey accessKey = applicationConfigStore.getAccessKeyStore()
                .getRequired(accessKeyId)
                .getValue();
        applicationConfigStore.getAccessKeyStore().remove(requestDTO.id());

        SecretKey secretKey = cryptoTunnel.secretKey(accessKey.key().getBytes());
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
        HandshakeUtils.validateHandshakeLiveTimeout(requestPayload.timestamp());

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
        String remoteFingerprint = CommonUtils.getFingerprint(publicKeyRSA);
        applicationConfigStore.getHandshakeStore().trustedInStore()
                .save(
                        remoteFingerprint,
                        new HandshakeTrustedInStore.TrustedIn(
                                rsaKeyPair.getPublic().getEncoded(),
                                rsaKeyPair.getPrivate().getEncoded(),
                                publicKeyRSA,
                                Instant.now()
                        )
                );

        byte[] publicKeyDH = requestPayload.publicKeyDH();
        applicationConfigStore.getHandshakeStore().sessionInStore()
                .save(
                        remoteFingerprint,
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
        HandshakeTrustedInStore.TrustedIn trustedIn = applicationConfigStore.getHandshakeStore()
                .trustedInStore()
                .getRequired(sessionDTO.fingerprint())
                .getValue();
        String remoteFingerprint = sessionDTO.fingerprint();
        HandshakeUtils.matchFingerprint(remoteFingerprint, CryptoRSA.getPublicKey(trustedIn.remoteRSA()));

        byte[] sessionPayloadDTOBytes = sessionDTO.payload();

        CryptoRSA.verify(
                sessionPayloadDTOBytes,
                sessionDTO.signature(),
                CryptoRSA.getPublicKey(trustedIn.remoteRSA())
        );

        HandshakeSessionDTO.SessionPayload sessionPayload = objectMapper.readValue(
                sessionPayloadDTOBytes, HandshakeSessionDTO.SessionPayload.class
        );
        HandshakeUtils.validateHandshakeLiveTimeout(sessionPayload.timestamp());

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

        applicationConfigStore.getHandshakeStore()
                .sessionInStore()
                .save(
                        remoteFingerprint,
                        new HandshakeSessionStore.SessionValue(
                                keyPairDH.getPublic().getEncoded(),
                                keyPairDH.getPrivate().getEncoded(),
                                sessionPayload.publicKey(),
                                Instant.now()
                        )
                );

        return sessionResponse;
    }
}
