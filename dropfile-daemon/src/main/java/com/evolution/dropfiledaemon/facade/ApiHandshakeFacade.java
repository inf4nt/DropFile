package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.LockableOperation;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.ApiHandshakeReconnectRequestDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeRequestDTO;
import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfiledaemon.crypto.CryptoConstants;
import com.evolution.dropfiledaemon.handshake.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.util.KeyEnvelopeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiHandshakeFacade {

    private final HandshakeClient handshakeClient;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    private final LockableOperation lockableOperationHandshakeTrustedOutStore;

    public void handshake(ApiHandshakeRequestDTO requestDTO) {
        try {
            URI addressURI = CommonUtils.toURI(requestDTO.address());
            Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> existingAddressURI = handshakeTrustedOutStore
                    .getByAddressURI(addressURI)
                    .orElse(null);
            if (existingAddressURI != null) {
                String existingFingerprint = existingAddressURI.getKey();

                if (!requestDTO.force()) {
                    throw new IllegalStateException(("Unable to process handshake request." +
                            " Duplicate address URI %s (fingerprint: %s)." +
                            " Try to perform disconnect or use --force option")
                            .formatted(addressURI, existingFingerprint));
                }

                disconnectByFingerprint(existingFingerprint);
            }

            KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();
            KeyPair dhKeyPair = CryptoECDH.generateKeyPair();

            UUID handshakeRequestId = UUID.randomUUID();
            byte[] clientSalt = CommonUtils.nonce16();
            HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                    handshakeRequestId,
                    clientSalt,
                    rsaKeyPair.getPublic().getEncoded(),
                    dhKeyPair.getPublic().getEncoded(),
                    System.currentTimeMillis()
            );
            byte[] requestPayloadByteArray = objectMapper.writeValueAsBytes(requestPayload);

            String rawSecret = requestDTO.key();
            String accessSecretKeyId = KeyEnvelopeUtils.getId(rawSecret);

            SecretKey secretHandshakeClientKey = cryptoTunnel.deriveSecretKey(
                    rawSecret.getBytes(StandardCharsets.UTF_8),
                    CryptoConstants.HANDSHAKE_SECRET_CLIENT_INFO
            );
            SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                    requestPayloadByteArray,
                    accessSecretKeyId.getBytes(StandardCharsets.UTF_8),
                    secretHandshakeClientKey
            );

            byte[] signature = CryptoRSA.sign(
                    requestPayloadByteArray,
                    rsaKeyPair.getPrivate()
            );

            HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                    accessSecretKeyId,
                    secureEnvelope.payload(),
                    secureEnvelope.nonce(),
                    signature
            );

            HandshakeResponseDTO handshakeResponseDTO = handshakeClient
                    .handshake(addressURI, handshakeRequestDTO);

            SecretKey secretHandshakeServerKey = cryptoTunnel.deriveSecretKey(
                    rawSecret.getBytes(StandardCharsets.UTF_8),
                    CryptoConstants.HANDSHAKE_SECRET_SERVER_INFO
            );
            byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                    handshakeResponseDTO.payload(),
                    handshakeResponseDTO.nonce(),
                    accessSecretKeyId.getBytes(StandardCharsets.UTF_8),
                    secretHandshakeServerKey
            );

            HandshakeResponseDTO.Payload responsePayload = objectMapper.readValue(
                    decryptResponsePayload,
                    HandshakeResponseDTO.Payload.class
            );

            CryptoRSA.verify(
                    decryptResponsePayload,
                    handshakeResponseDTO.signature(),
                    CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
            );
            if (!handshakeRequestId.equals(responsePayload.requestId())) {
                throw new SecurityException("Handshake response handshakeId mismatch! Expected %s, got %s"
                        .formatted(handshakeRequestId, responsePayload.requestId()));
            }

            byte[] sessionRawKey = CryptoECDH.getSecretKey(
                    CryptoECDH.getPrivateKey(dhKeyPair.getPrivate().getEncoded()),
                    CryptoECDH.getPublicKey(responsePayload.publicKeyDH())
            );

            byte[] salt = CommonUtils.salt(clientSalt, responsePayload.serverSalt());
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

            String remoteFingerprint = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());
            lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(remoteFingerprint, () -> {
                Instant now = Instant.now();
                handshakeTrustedOutStore.save(remoteFingerprint, () -> new HandshakeTrustedOutStore.TrustedOut(
                        addressURI,
                        new HandshakeTrustedOutStore.HandshakeKeys(
                                rsaKeyPair.getPublic().getEncoded(),
                                rsaKeyPair.getPrivate().getEncoded(),
                                responsePayload.publicKeyRSA()
                        ),
                        now,
                        now,
                        now,
                        now,
                        handshakeRequestId
                ));
                handshakeSessionOutStore.save(remoteFingerprint, () -> new HandshakeSessionOutStore.SessionOut(
                        dhKeyPair.getPublic().getEncoded(),
                        dhKeyPair.getPrivate().getEncoded(),
                        responsePayload.publicKeyDH(),
                        secretTunnelClientKey.getEncoded(),
                        secretTunnelServerKey.getEncoded(),
                        handshakeRequestId
                ));
            });
        } catch (Exception e) {
            throw CommonUtils.toRuntimeException(e.getMessage(), e);
        }
    }

    private void handshakeReconnect(String fingerprint, boolean byUser) {
        try {
            lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(fingerprint, () -> {
                HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore.getRequired(fingerprint).getValue();

                URI addressURI = trustedOut.addressURI();

                KeyPair keyPairDH = CryptoECDH.generateKeyPair();

                UUID sessionRequestId = UUID.randomUUID();
                byte[] clientSalt = CommonUtils.nonce16();

                HandshakeSessionDTO.SessionRequestPayload sessionPayloadRequest = new HandshakeSessionDTO.SessionRequestPayload(
                        sessionRequestId,
                        clientSalt,
                        keyPairDH.getPublic().getEncoded(),
                        System.currentTimeMillis()
                );
                byte[] sessionPayloadRequestBytes = objectMapper.writeValueAsBytes(sessionPayloadRequest);
                byte[] signature = CryptoRSA.sign(
                        sessionPayloadRequestBytes,
                        CryptoRSA.getPrivateKey(trustedOut.handshake().privateRSA())
                );

                HandshakeSessionDTO.Session sessionRequest = new HandshakeSessionDTO.Session(
                        CommonUtils.getFingerprint(trustedOut.handshake().publicRSA()),
                        sessionPayloadRequestBytes,
                        signature
                );
                HandshakeSessionDTO.Session sessionResponse = handshakeClient.handshakeSession(addressURI, sessionRequest);

                HandshakeSessionDTO.SessionResponsePayload sessionResponsePayload = objectMapper.readValue(
                        sessionResponse.payload(),
                        HandshakeSessionDTO.SessionResponsePayload.class
                );

                String remoteFingerprint = sessionResponse.fingerprint();
                matchFingerprint(remoteFingerprint, CryptoRSA.getPublicKey(trustedOut.handshake().remoteRSA()));
                CryptoRSA.verify(
                        sessionResponse.payload(),
                        sessionResponse.signature(),
                        CryptoRSA.getPublicKey(trustedOut.handshake().remoteRSA())
                );

                if (!sessionRequestId.equals(sessionResponsePayload.requestId())) {
                    throw new SecurityException("Session response handshakeId mismatch! Expected %s, got %s"
                            .formatted(sessionRequestId, sessionResponsePayload.requestId()));
                }

                byte[] sessionRawKey = CryptoECDH.getSecretKey(
                        CryptoECDH.getPrivateKey(keyPairDH.getPrivate().getEncoded()),
                        CryptoECDH.getPublicKey(sessionResponsePayload.publicKeyDH())
                );

                byte[] salt = CommonUtils.salt(clientSalt, sessionResponsePayload.serverSalt());

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

                handshakeTrustedOutStore.update(remoteFingerprint, value -> {
                    Instant now = Instant.now();
                    HandshakeTrustedOutStore.TrustedOut next = value
                            .withHandshakeId(sessionRequestId)
                            .withUpdated(now);
                    next = byUser ? next.withSessionUpdatedByUser(now) : next.withSessionUpdatedBySystem(now);
                    return next;
                });

                handshakeSessionOutStore.save(remoteFingerprint, () -> new HandshakeSessionOutStore.SessionOut(
                        keyPairDH.getPublic().getEncoded(),
                        keyPairDH.getPrivate().getEncoded(),
                        sessionResponsePayload.publicKeyDH(),
                        secretTunnelClientKey.getEncoded(),
                        secretTunnelServerKey.getEncoded(),
                        sessionRequestId
                ));
            });
        } catch (Exception e) {
            throw CommonUtils.toRuntimeException(e.getMessage(), e);
        }
    }

    public void handshakeReconnect(ApiHandshakeReconnectRequestDTO requestDTO) {
        String fingerprint = handshakeTrustedOutStore
                .getRequiredByAddressURI(CommonUtils.toURI(requestDTO.address())).getKey();
        handshakeReconnect(fingerprint, true);
    }

    public void systemHandshakeReconnect(String fingerprint) {
        handshakeReconnect(fingerprint, false);
    }

    public void handshakeCurrentReconnect() {
        String fingerprint = handshakeTrustedOutStore.getRequiredLastUpdated().getKey();
        handshakeReconnect(fingerprint, true);
    }

    public void disconnect(CriteriaEnvelope fingerprintCriteria) {
        String fingerprint = handshakeTrustedOutStore.getRequiredByCriteria(fingerprintCriteria).getKey();
        disconnectByFingerprint(fingerprint);
    }

    private void disconnectByFingerprint(String fingerprint) {
        lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(fingerprint, () -> {
            handshakeTrustedOutStore.remove(fingerprint);
        });
    }

    public void disconnectCurrent() {
        String fingerprint = handshakeTrustedOutStore.getRequiredLastUpdated().getKey();
        disconnectByFingerprint(fingerprint);
    }

    public void disconnectAll() {
        lockableOperationHandshakeTrustedOutStore.executeWithGlobalLock(() -> {
            handshakeTrustedOutStore.removeAll();
        });
    }

    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        Map<String, HandshakeTrustedOutStore.TrustedOut> trusts = handshakeTrustedOutStore.getAll();
        return mapToHandshakeApiTrustOutResponseDTOList(trusts);
    }

    public HandshakeApiTrustOutResponseDTO getLatestTrustOut() {
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> lastHandshake = handshakeTrustedOutStore.getRequiredLastUpdated();
        return mapToHandshakeApiTrustOutResponseDTO(lastHandshake.getKey(), lastHandshake.getValue());
    }

    private List<HandshakeApiTrustOutResponseDTO> mapToHandshakeApiTrustOutResponseDTOList(Map<String, HandshakeTrustedOutStore.TrustedOut> trusts) {
        return trusts.entrySet().stream()
                .map(entry -> {
                    String remoteFingerprint = entry.getKey();
                    HandshakeTrustedOutStore.TrustedOut trustedOut = entry.getValue();
                    return mapToHandshakeApiTrustOutResponseDTO(remoteFingerprint, trustedOut);
                })
                .toList();
    }

    private HandshakeApiTrustOutResponseDTO mapToHandshakeApiTrustOutResponseDTO(String remoteFingerprint,
                                                                                 HandshakeTrustedOutStore.TrustedOut trustedOut) {
        HandshakeSessionOutStore.SessionOut sessionOut = handshakeSessionOutStore
                .get(remoteFingerprint)
                .map(it -> it.getValue())
                .filter(it -> it.handshakeId().equals(trustedOut.handshakeId()))
                .orElse(null);

        return new HandshakeApiTrustOutResponseDTO(
                remoteFingerprint,
                CommonUtils.encodeBase64(trustedOut.handshake().publicRSA()),
                CommonUtils.encodeBase64(trustedOut.handshake().remoteRSA()),
                sessionOut == null ? null : CommonUtils.encodeBase64(sessionOut.publicDH()),
                sessionOut == null ? null : CommonUtils.encodeBase64(sessionOut.remotePublicDH()),
                trustedOut.addressURI().toString(),
                trustedOut.created(),
                trustedOut.updated()
        );
    }

    private void matchFingerprint(String fingerprint, PublicKey publicKey) {
        String fingerprintExpected = CommonUtils.getFingerprint(publicKey.getEncoded());
        if (!fingerprintExpected.equals(fingerprint)) {
            throw new SecurityException(String.format(
                    "Fingerprint mismatch %s vs %s", fingerprint, fingerprintExpected
            ));
        }
    }
}
