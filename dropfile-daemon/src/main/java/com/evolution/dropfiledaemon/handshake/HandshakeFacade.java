package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.LockableOperation;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.HandshakeApiTrustInResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfiledaemon.crypto.CryptoConstants;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.service.ReplyAttackGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class HandshakeFacade {

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    private final AccessKeyStore accessKeyStore;

    private final LockableOperation lockableOperationHandshakeTrustedInStore;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final HandshakeSessionInStore handshakeSessionInStore;

    private final ReplyAttackGuard replyAttackGuard;

    @SneakyThrows
    public HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.accessKeyId();

        AccessKey accessKey = accessKeyStore.remove(accessKeyId);
        if (accessKey == null) {
            throw new SecurityException("Access key %s not found or already consumed".formatted(accessKeyId));
        }

        String rawSecret = accessKey.key();
        byte[] rawSecretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        byte[] aad = accessKeyId.getBytes(StandardCharsets.UTF_8);

        SecretKey secretHandshakeClientKey = cryptoTunnel.deriveSecretKey(
                rawSecretBytes,
                CryptoConstants.HANDSHAKE_SECRET_CLIENT_INFO
        );
        SecretKey secretHandshakeServerKey = cryptoTunnel.deriveSecretKey(
                rawSecretBytes,
                CryptoConstants.HANDSHAKE_SECRET_SERVER_INFO
        );

        byte[] decryptMessage = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                aad,
                secretHandshakeClientKey
        );

        HandshakeRequestDTO.Payload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.Payload.class);

        CryptoRSA.verify(
                decryptMessage,
                requestDTO.signature(),
                CryptoRSA.getPublicKey(requestPayload.publicKeyRSA())
        );
        replyAttackGuard.handshakeRequest(requestPayload);

        KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();
        KeyPair dhKeyPair = CryptoECDH.generateKeyPair();

        byte[] serverSalt = CommonUtils.nonce16();
        UUID requestId = requestPayload.requestId();

        HandshakeResponseDTO.Payload responsePayload = new HandshakeResponseDTO.Payload(
                requestId,
                serverSalt,
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
                aad,
                secretHandshakeServerKey
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

            byte[] sessionRawKey = CryptoECDH.getSecretKey(
                    CryptoECDH.getPrivateKey(dhKeyPair.getPrivate().getEncoded()),
                    CryptoECDH.getPublicKey(publicKeyDH)
            );

            byte[] salt = CommonUtils.salt(requestPayload.clientSalt(), serverSalt);
            SecretKey secretTunnelClientKey = cryptoTunnel.deriveSecretKey(
                    sessionRawKey,
                    CryptoConstants.TUNNEL_SECRET_CLIENT_INFO,
                    salt
            );
            SecretKey secretTunnelServerKey = cryptoTunnel.deriveSecretKey(
                    sessionRawKey,
                    CryptoConstants.TUNNEL_SECRET_SERVER_INFO,
                    salt
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
                                now,
                                now,
                                now,
                                requestId
                        );
                    }
            );

            handshakeSessionInStore.save(remoteFingerprint, () -> new HandshakeSessionInStore.SessionIn(
                    dhKeyPair.getPublic().getEncoded(),
                    dhKeyPair.getPrivate().getEncoded(),
                    publicKeyDH,
                    secretTunnelClientKey.getEncoded(),
                    secretTunnelServerKey.getEncoded(),
                    requestId
            ));

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

            replyAttackGuard.sessionRequest(sessionPayloadRequest);

            KeyPair keyPairDH = CryptoECDH.generateKeyPair();

            byte[] serverSalt = CommonUtils.nonce16();

            HandshakeSessionDTO.SessionResponsePayload sessionPayloadResponse = new HandshakeSessionDTO.SessionResponsePayload(
                    sessionPayloadRequest.requestId(),
                    serverSalt,
                    keyPairDH.getPublic().getEncoded()
            );
            byte[] sessionPayloadResponseBytes = objectMapper.writeValueAsBytes(sessionPayloadResponse);
            byte[] signature = CryptoRSA.sign(
                    sessionPayloadResponseBytes,
                    CryptoRSA.getPrivateKey(trustedIn.handshake().privateRSA())
            );

            byte[] sessionRawKey = CryptoECDH.getSecretKey(
                    CryptoECDH.getPrivateKey(keyPairDH.getPrivate().getEncoded()),
                    CryptoECDH.getPublicKey(sessionPayloadRequest.publicKeyDH())
            );

            byte[] salt = CommonUtils.salt(sessionPayloadRequest.clientSalt(), serverSalt);
            SecretKey secretTunnelClientKey = cryptoTunnel.deriveSecretKey(
                    sessionRawKey,
                    CryptoConstants.TUNNEL_SECRET_CLIENT_INFO,
                    salt
            );
            SecretKey secretTunnelServerKey = cryptoTunnel.deriveSecretKey(
                    sessionRawKey,
                    CryptoConstants.TUNNEL_SECRET_SERVER_INFO,
                    salt
            );

            HandshakeSessionDTO.Session sessionResponse = new HandshakeSessionDTO.Session(
                    CommonUtils.getFingerprint(trustedIn.handshake().publicRSA()),
                    sessionPayloadResponseBytes,
                    signature
            );

            UUID requestId = sessionPayloadRequest.requestId();

            handshakeTrustedInStore.update(fingerprint, value -> {
                Instant now = Instant.now();
                return value
                        .withSessionUpdated(now)
                        .withUpdated(now)
                        .withHandshakeId(requestId);
            });

            handshakeSessionInStore.save(fingerprint, () -> new HandshakeSessionInStore.SessionIn(
                    keyPairDH.getPublic().getEncoded(),
                    keyPairDH.getPrivate().getEncoded(),
                    sessionPayloadRequest.publicKeyDH(),
                    secretTunnelClientKey.getEncoded(),
                    secretTunnelServerKey.getEncoded(),
                    requestId
            ));

            return sessionResponse;
        });
    }

    public void revoke(CriteriaEnvelope fingerprintCriteria) {
        String fingerprint = handshakeTrustedInStore.getRequiredByCriteria(fingerprintCriteria)
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
            HandshakeSessionInStore.SessionIn sessionIn = handshakeSessionInStore.get(remoteFingerprint)
                    .map(it -> it.getValue())
                    .filter(it -> it.handshakeId().equals(trustedIn.handshakeId()))
                    .orElse(null);

            // TODO add updated by user/system
            return new HandshakeApiTrustInResponseDTO(
                    remoteFingerprint,
                    CommonUtils.encodeBase64(trustedIn.handshake().publicRSA()),
                    CommonUtils.encodeBase64(trustedIn.handshake().remoteRSA()),
                    sessionIn == null ? null : CommonUtils.encodeBase64(sessionIn.publicDH()),
                    sessionIn == null ? null : CommonUtils.encodeBase64(sessionIn.remotePublicDH()),
                    trustedIn.created(),
                    trustedIn.updated()
            );
        }).toList();
    }
}
