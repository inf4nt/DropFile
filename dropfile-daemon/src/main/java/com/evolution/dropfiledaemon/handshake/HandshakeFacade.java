package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.LockableOperation;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.HandshakeApiTrustInResponseDTO;
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
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class HandshakeFacade {

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    private final AccessKeyStore accessKeyStore;

    private final LockableOperation lockableOperationHandshakeTrustedInStore;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ReplyAttackGuard replyAttackGuard;

    @SneakyThrows
    public HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.accessKeyId();

        AccessKey accessKey = accessKeyStore
                .remove(accessKeyId);
        if (accessKey == null) {
            throw new SecurityException("Access key %s not found or already consumed".formatted(accessKeyId));
        }

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
                requestPayload.requestId(),
                rsaKeyPair.getPublic().getEncoded(),
                dhKeyPair.getPublic().getEncoded()
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
        return lockableOperationHandshakeTrustedInStore.executeWithKeyLock(remoteFingerprint, () -> {

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
        });
    }

    @SneakyThrows
    public HandshakeSessionDTO.Session handshakeSession(HandshakeSessionDTO.Session sessionDTO) {
        String fingerprint = sessionDTO.fingerprint();
        return lockableOperationHandshakeTrustedInStore.executeWithKeyLock(fingerprint, () -> {
            HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore.getRequired(fingerprint).getValue();

            byte[] sessionPayloadDTOBytes = sessionDTO.payload();
            HandshakeSessionDTO.SessionRequestPayload sessionPayloadRequest = objectMapper.readValue(
                    sessionPayloadDTOBytes, HandshakeSessionDTO.SessionRequestPayload.class
            );

            CryptoRSA.verify(
                    sessionPayloadDTOBytes,
                    sessionDTO.signature(),
                    CryptoRSA.getPublicKey(trustedIn.handshake().remoteRSA())
            );

            replyAttackGuard.tryToAddSessionRequest(sessionPayloadRequest);

            KeyPair keyPairDH = CryptoECDH.generateKeyPair();

            HandshakeSessionDTO.SessionResponsePayload sessionPayloadResponse = new HandshakeSessionDTO.SessionResponsePayload(
                    sessionPayloadRequest.requestId(),
                    keyPairDH.getPublic().getEncoded()
            );
            byte[] sessionPayloadResponseBytes = objectMapper.writeValueAsBytes(sessionPayloadResponse);
            byte[] signature = CryptoRSA.sign(
                    sessionPayloadResponseBytes,
                    CryptoRSA.getPrivateKey(trustedIn.handshake().privateRSA())
            );

            byte[] sessionKey = CryptoECDH.getSecretKey(
                    CryptoECDH.getPrivateKey(keyPairDH.getPrivate().getEncoded()),
                    CryptoECDH.getPublicKey(sessionPayloadRequest.publicKeyDH())
            );

            HandshakeSessionDTO.Session sessionResponse = new HandshakeSessionDTO.Session(
                    CommonUtils.getFingerprint(trustedIn.handshake().publicRSA()),
                    sessionPayloadResponseBytes,
                    signature
            );

            handshakeTrustedInStore.update(fingerprint, value -> {
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
        });
    }

    public void revoke(String mightFingerprint) {
        String fingerprint = handshakeTrustedInStore.getRequiredByKeyStartWith(mightFingerprint)
                .getKey();
        lockableOperationHandshakeTrustedInStore.executeWithKeyLock(fingerprint, () -> {
            handshakeTrustedInStore.remove(fingerprint);
        });
    }

    public void revokeAll() {
        lockableOperationHandshakeTrustedInStore.executeWithGlobalLock(() -> {
            handshakeTrustedInStore.removeAll();
        });
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        Map<String, HandshakeTrustedInStore.TrustedIn> trusts = handshakeTrustedInStore.getAll();
        return mapToHandshakeApiTrustInResponseDTOList(trusts);
    }

    private List<HandshakeApiTrustInResponseDTO> mapToHandshakeApiTrustInResponseDTOList(Map<String, HandshakeTrustedInStore.TrustedIn> trusts) {
        return trusts.entrySet().stream().map(entry -> {
            String remoteFingerprint = entry.getKey();
            HandshakeTrustedInStore.TrustedIn trustedIn = entry.getValue();

            // TODO add updated by user/system
            return new HandshakeApiTrustInResponseDTO(
                    remoteFingerprint,
                    CommonUtils.encodeBase64(trustedIn.handshake().publicRSA()),
                    CommonUtils.encodeBase64(trustedIn.handshake().remoteRSA()),
                    CommonUtils.encodeBase64(trustedIn.session().publicDH()),
                    CommonUtils.encodeBase64(trustedIn.session().remotePublicDH()),
                    trustedIn.created(),
                    trustedIn.updated()
            );
        }).toList();
    }
}
