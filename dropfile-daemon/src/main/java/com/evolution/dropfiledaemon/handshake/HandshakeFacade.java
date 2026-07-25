package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.service.ReplyAttackGuard;
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

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    private final AccessKeyStore accessKeyStore;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ReplyAttackGuard replyAttackGuard;

    @SneakyThrows
    public synchronized HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();

        AccessKey accessKey = accessKeyStore
                .remove(accessKeyId);

        String rawSecret = accessKey.key();
        SecretKey secretKey = cryptoTunnel.secretKey(rawSecret.getBytes());

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
        replyAttackGuard.tryToAddHandshakeRequest(requestPayload);

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
        byte[] publicKeyDH = requestPayload.publicKeyDH();

        byte[] sessionKey = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(dhKeyPair.getPrivate().getEncoded()),
                CryptoECDH.getPublicKey(publicKeyDH)
        );

        handshakeTrustedInStore.save(
                remoteFingerprint,
                () -> {
                    Instant now = Instant.now();
                    return new HandshakeTrustedInStore.TrustedIn(
                            new HandshakeTrustedInStore.HandshakeKeys(
                                    rsaKeyPair.getPublic().getEncoded(),
                                    rsaKeyPair.getPrivate().getEncoded(),
                                    publicKeyRSA
                            ),
                            new HandshakeTrustedInStore.SessionKeys(
                                    dhKeyPair.getPublic().getEncoded(),
                                    dhKeyPair.getPrivate().getEncoded(),
                                    publicKeyDH,
                                    sessionKey
                            ),
                            now,
                            now,
                            now
                    );
                }
        );
        return handshakeResponseDTO;
    }

    @SneakyThrows
    public synchronized HandshakeSessionDTO.Session handshakeSession(HandshakeSessionDTO.Session sessionDTO) {
        String remoteFingerprint = sessionDTO.fingerprint();

        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore
                .getRequired(remoteFingerprint)
                .getValue();

        byte[] sessionPayloadDTOBytes = sessionDTO.payload();

        HandshakeSessionDTO.SessionPayload sessionPayloadRequest = objectMapper.readValue(
                sessionPayloadDTOBytes, HandshakeSessionDTO.SessionPayload.class
        );

        CryptoRSA.verify(
                sessionPayloadDTOBytes,
                sessionDTO.signature(),
                CryptoRSA.getPublicKey(trustedIn.handshake().remoteRSA())
        );

        replyAttackGuard.tryToAddSessionRequest(sessionPayloadRequest);

        KeyPair keyPairDH = CryptoECDH.generateKeyPair();

        HandshakeSessionDTO.SessionPayload sessionPayloadResponse = new HandshakeSessionDTO.SessionPayload(
                keyPairDH.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] sessionPayloadResponseBytes = objectMapper.writeValueAsBytes(sessionPayloadResponse);
        byte[] signature = CryptoRSA.sign(
                sessionPayloadResponseBytes,
                CryptoRSA.getPrivateKey(trustedIn.handshake().privateRSA())
        );

        HandshakeSessionDTO.Session sessionResponse = new HandshakeSessionDTO.Session(
                CommonUtils.getFingerprint(trustedIn.handshake().publicRSA()),
                sessionPayloadResponseBytes,
                signature
        );

        byte[] sessionKey = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keyPairDH.getPrivate().getEncoded()),
                CryptoECDH.getPublicKey(sessionPayloadRequest.publicKeyDH())
        );

        handshakeTrustedInStore.update(remoteFingerprint, value -> {
            Instant now = Instant.now();
            return value
                    .withSession(
                            new HandshakeTrustedInStore.SessionKeys(
                                    keyPairDH.getPublic().getEncoded(),
                                    keyPairDH.getPrivate().getEncoded(),
                                    sessionPayloadRequest.publicKeyDH(),
                                    sessionKey
                            )
                    )
                    .withSessionUpdated(now)
                    .withUpdated(now);
        });

        return sessionResponse;
    }
}