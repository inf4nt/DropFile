package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.handshake.HandshakeHelper;
import com.evolution.dropfiledaemon.handshake.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.util.KeyEnvelopeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
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

    private final ApiConnectionsFacade apiConnectionsFacade;

    private final HandshakeClient handshakeClient;

    private final HandshakeHelper handshakeHelper;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    @SneakyThrows
    public synchronized ApiHandshakeStatusResponseDTO handshake(ApiHandshakeRequestDTO requestDTO) {
        URI addressURI = CommonUtils.toURI(requestDTO.address());
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> existingAddressURI = handshakeTrustedOutStore
                .getByAddressURI(addressURI)
                .orElse(null);
        if (existingAddressURI != null) {
            if (!requestDTO.force()) {
                throw new RuntimeException(String.format(
                        "Unable to process handshake request. Duplicate address URI %s fingerprint %s. Try to perform disconnect or --force option",
                        addressURI, existingAddressURI.getKey()
                ));
            }
            apiConnectionsFacade.disconnect(existingAddressURI.getKey());
        }

        KeyPair rsaKeyPair = CryptoRSA.generateKeyPair();
        KeyPair dhKeyPair = CryptoECDH.generateKeyPair();

        HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                rsaKeyPair.getPublic().getEncoded(),
                dhKeyPair.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] requestPayloadByteArray = objectMapper.writeValueAsBytes(requestPayload);

        String rawSecret = requestDTO.key();
        SecretKey secretKey = cryptoTunnel.secretKey(rawSecret.getBytes());
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                requestPayloadByteArray,
                secretKey
        );

        byte[] signature = CryptoRSA.sign(
                requestPayloadByteArray,
                rsaKeyPair.getPrivate()
        );

        String requestId = KeyEnvelopeUtils.getId(rawSecret);
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
        handshakeHelper.validateHandshakeLiveTimeout(responsePayload.timestamp());

        CryptoRSA.verify(
                decryptResponsePayload,
                handshakeResponseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );

        String remoteFingerprint = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());

        Instant createInstantTime = Instant.now();
        handshakeTrustedOutStore
                .save(
                        remoteFingerprint,
                        new HandshakeTrustedOutStore.TrustedOut(
                                addressURI,
                                new HandshakeTrustedOutStore.HandshakeKeys(
                                        rsaKeyPair.getPublic().getEncoded(),
                                        rsaKeyPair.getPrivate().getEncoded(),
                                        responsePayload.publicKeyRSA()
                                ),
                                new HandshakeTrustedOutStore.SessionKeys(
                                        dhKeyPair.getPublic().getEncoded(),
                                        dhKeyPair.getPrivate().getEncoded(),
                                        responsePayload.publicKeyDH()
                                ),
                                null, // TODO implement TTL
                                null,
                                createInstantTime,
                                0,
                                createInstantTime,
                                null,
                                createInstantTime
                        )
                );
        return new ApiHandshakeStatusResponseDTO(
                remoteFingerprint,
                addressURI.toString()
        );
    }

    @SneakyThrows
    public synchronized ApiHandshakeStatusResponseDTO handshakeReconnect(ApiHandshakeReconnectRequestDTO requestDTO) {
        HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore
                .getRequiredByAddressURI(CommonUtils.toURI(requestDTO.address()))
                .getValue();
        return handshakeReconnect(trustedOut, true, null);
    }

    public synchronized ApiHandshakeStatusResponseDTO systemHandshakeSessionRefresh(String fingerprint, long sessionRefreshRequestTimestamp) {
        HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore
                .getRequired(fingerprint).getValue();
        return handshakeReconnect(trustedOut, false, sessionRefreshRequestTimestamp);
    }

    @SneakyThrows
    public synchronized ApiHandshakeStatusResponseDTO handshakeReconnect(HandshakeTrustedOutStore.TrustedOut trustedOut,
                                                                         boolean byUser,
                                                                         @Nullable Long sessionRefreshRequestTimestamp) {
        URI addressURI = trustedOut.addressURI();

        KeyPair keyPairDH = CryptoECDH.generateKeyPair();
        HandshakeSessionDTO.SessionPayload sessionPayloadRequest = new HandshakeSessionDTO.SessionPayload(
                keyPairDH.getPublic().getEncoded(),
                System.currentTimeMillis()
        );
        byte[] sessionPayloadRequestBytes = objectMapper.writeValueAsBytes(sessionPayloadRequest);
        byte[] signature = CryptoRSA.sign(sessionPayloadRequestBytes, CryptoRSA.getPrivateKey(trustedOut.handshake().privateRSA()));
        HandshakeSessionDTO.Session sessionRequest = new HandshakeSessionDTO.Session(
                CommonUtils.getFingerprint(trustedOut.handshake().publicRSA()),
                sessionPayloadRequestBytes,
                signature
        );
        HandshakeSessionDTO.Session sessionResponse = handshakeClient.handshakeSession(addressURI, sessionRequest);

        CryptoRSA.verify(
                sessionResponse.payload(),
                sessionResponse.signature(),
                CryptoRSA.getPublicKey(trustedOut.handshake().remoteRSA())
        );
        HandshakeSessionDTO.SessionPayload sessionPayload = objectMapper.readValue(sessionResponse.payload(), HandshakeSessionDTO.SessionPayload.class);

        handshakeHelper.validateHandshakeLiveTimeout(sessionPayload.timestamp());

        String remoteFingerprint = sessionResponse.fingerprint();
        handshakeHelper.matchFingerprint(remoteFingerprint, CryptoRSA.getPublicKey(trustedOut.handshake().remoteRSA()));

        handshakeTrustedOutStore.update(remoteFingerprint, value -> {
            Instant now = Instant.now();
            HandshakeTrustedOutStore.TrustedOut next = value
                    .withSession(new HandshakeTrustedOutStore.SessionKeys(
                            keyPairDH.getPublic().getEncoded(),
                            keyPairDH.getPrivate().getEncoded(),
                            sessionPayload.publicKey()
                    ))
                    .withUpdated(now);
            if (byUser) {
                next = next.withSessionUpdatedByUser(now);
            } else {
                next = next.withSessionUpdatedBySystem(now);
            }

            // TODO can i use this timestamp instead of sessionRefreshRequestTimestamp ?
            long timestamp = sessionPayload.timestamp();

            if (sessionRefreshRequestTimestamp != null) {
                next = next.withSessionRefreshRequestTimestamp(sessionRefreshRequestTimestamp);
            }
            return next;
        });

        return new ApiHandshakeStatusResponseDTO(
                remoteFingerprint,
                addressURI.toString()
        );
    }

    public synchronized ApiHandshakeStatusResponseDTO handshakeStatus() {
        Map.Entry<String, HandshakeTrustedOutStore.TrustedOut> lastHandshake = handshakeTrustedOutStore.getRequiredLastUpdated();
        URI addressURI = lastHandshake.getValue().addressURI();
        return handshakeReconnect(new ApiHandshakeReconnectRequestDTO(addressURI.toString()));
    }

    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        Map<String, HandshakeTrustedOutStore.TrustedOut> trusts = handshakeTrustedOutStore.getAll();
        return mapToHandshakeApiTrustOutResponseDTOList(trusts);
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        Map<String, HandshakeTrustedInStore.TrustedIn> trusts = handshakeTrustedInStore.getAll();
        return mapToHandshakeApiTrustInResponseDTOList(trusts);
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
                    trustedIn.updatedByUser()
            );
        }).toList();
    }

    private HandshakeApiTrustOutResponseDTO mapToHandshakeApiTrustOutResponseDTO(String remoteFingerprint, HandshakeTrustedOutStore.TrustedOut trustedOut) {
        return new HandshakeApiTrustOutResponseDTO(
                remoteFingerprint,
                CommonUtils.encodeBase64(trustedOut.handshake().publicRSA()),
                CommonUtils.encodeBase64(trustedOut.handshake().remoteRSA()),
                CommonUtils.encodeBase64(trustedOut.session().publicDH()),
                CommonUtils.encodeBase64(trustedOut.session().remotePublicDH()),
                trustedOut.addressURI().toString(),
                trustedOut.created(),
                trustedOut.updated()
        );
    }
}
