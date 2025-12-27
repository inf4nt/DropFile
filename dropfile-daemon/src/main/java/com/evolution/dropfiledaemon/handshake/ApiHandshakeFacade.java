package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.exception.NoDaemonPublicAddressException;
import com.evolution.dropfiledaemon.handshake.exception.NoIncomingRequestFoundException;
import com.evolution.dropfiledaemon.handshake.store.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ApiHandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final HandshakeClient handshakeClient;

    private final ObjectMapper objectMapper;

    private final KeysConfigStore keysConfigStore;

    private final AppConfigStore appConfigStore;

    public ApiHandshakeFacade(HandshakeStore handshakeStore,
                              HandshakeClient handshakeClient,
                              ObjectMapper objectMapper,
                              KeysConfigStore keysConfigStore,
                              AppConfigStore appConfigStore) {
        this.handshakeStore = handshakeStore;
        this.handshakeClient = handshakeClient;
        this.objectMapper = objectMapper;
        this.keysConfigStore = keysConfigStore;
        this.appConfigStore = appConfigStore;
    }

    public List<HandshakeApiIncomingResponseDTO> getIncomingRequests() {
        return handshakeStore
                .incomingRequestStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    URI addressURI = entry.getValue().addressURI();
                    byte[] publicKey = entry.getValue().publicKey();
                    String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
                    return new HandshakeApiIncomingResponseDTO(fingerprint, publicKeyBase64, addressURI.toString());
                })
                .toList();
    }

    public List<HandshakeApiOutgoingResponseDTO> getOutgoingRequests() {
        return handshakeStore
                .outgoingRequestStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    URI addressURI = entry.getValue().addressURI();
                    byte[] publicKey = entry.getValue().publicKey();
                    String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
                    return new HandshakeApiOutgoingResponseDTO(fingerprint, publicKeyBase64, addressURI.toString());
                })
                .toList();
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        return handshakeStore
                .trustedInStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    URI addressURI = entry.getValue().addressURI();
                    byte[] publicKey = entry.getValue().publicKey();
                    String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
                    return new HandshakeApiTrustInResponseDTO(fingerprint, publicKeyBase64, addressURI.toString());
                })
                .toList();
    }

    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        return handshakeStore
                .trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    URI addressURI = entry.getValue().addressURI();
                    byte[] publicKey = entry.getValue().publicKey();
                    String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
                    return new HandshakeApiTrustOutResponseDTO(fingerprint, publicKeyBase64, addressURI.toString());
                })
                .toList();
    }

    public Optional<HandshakeApiTrustOutResponseDTO> getLatestTrustOut() {
        return handshakeStore.trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().updated().compareTo(o1.getValue().updated()))
                .findFirst()
                .map(it -> new HandshakeApiTrustOutResponseDTO(
                                it.getKey(),
                                CryptoUtils.encodeBase64(it.getValue().publicKey()),
                                it.getValue().addressURI().toString()
                        )
                );
    }

    public void trust(String fingerprint) {
        IncomingRequestKeyValueStore.IncomingRequestValue incomingRequestValue = handshakeStore
                .incomingRequestStore()
                .get(fingerprint)
                .orElseThrow(() -> new NoIncomingRequestFoundException(fingerprint));
        handshakeStore.incomingRequestStore().remove(fingerprint);
        String secret = UUID.randomUUID().toString();
        handshakeStore.trustedInStore()
                .save(
                        fingerprint,
                        new TrustedInKeyValueStore.TrustedInValue(
                                incomingRequestValue.addressURI(),
                                incomingRequestValue.publicKey(),
                                secret
                        )
                );
    }

    @SneakyThrows
    public HandshakeApiRequestResponseStatus initializeRequest(HandshakeApiRequestBodyDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.nodeAddress());
        String currentFingerPrint = CryptoUtils.getFingerPrint(keysConfigStore.getRequired().publicKey());
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            return doHandshakeChallenge(requestBody.publicKey(), nodeAddressURI, handshakeResponse);
        } else if (handshakeResponse.statusCode() == 404) {
            return doHandshakeRequest(requestBody.publicKey(), nodeAddressURI);
        }
        throw new RuntimeException(
                "Unexpected handshake trust request response code  " + handshakeResponse.statusCode()
        );
    }

    @SneakyThrows
    private HandshakeApiRequestResponseStatus doHandshakeRequest(String expectedPublicKeyBase64,
                                                                 URI nodeAddressURI) {
        AppConfig.DaemonAppConfig daemonAppConfig = appConfigStore.getRequired().daemonAppConfig();

        URI publicDaemonAddressURI = daemonAppConfig.publicDaemonAddressURI();
        if (publicDaemonAddressURI == null) {
            throw new NoDaemonPublicAddressException();
        }

        HttpResponse<byte[]> httpResponse = handshakeClient.handshakeRequest(
                publicDaemonAddressURI,
                nodeAddressURI,
                keysConfigStore.getRequired().publicKey()
        );

        if (httpResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake request response code: " + httpResponse.statusCode());
        }

        HandshakeRequestResponseDTO responseDTO = objectMapper
                .readValue(httpResponse.body(), HandshakeRequestResponseDTO.class);
        byte[] publicKeyBytes = CryptoUtils.decodeBase64(responseDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKeyBytes);
        String fingerprintExpected = CryptoUtils.getFingerPrint(CryptoUtils.decodeBase64(expectedPublicKeyBase64));

        if (!fingerprintExpected.equals(fingerPrint)) {
            return HandshakeApiRequestResponseStatus.FINGERPRINT_MISMATCH;
        }

        handshakeStore.outgoingRequestStore().save(
                fingerPrint,
                new OutgoingRequestKeyValueStore.OutgoingRequestValue(
                        nodeAddressURI,
                        publicKeyBytes
                )
        );
        return HandshakeApiRequestResponseStatus.PENDING;
    }

    @SneakyThrows
    private HandshakeApiRequestResponseStatus doHandshakeChallenge(String publicKeyExpectedBase64,
                                                                   URI nodeAddressURI,
                                                                   HttpResponse<byte[]> handshakeResponse) {
        HandshakeTrustResponseDTO handshakeTrustResponseDTO = objectMapper
                .readValue(handshakeResponse.body(), HandshakeTrustResponseDTO.class);
        byte[] publicKey = CryptoUtils.decodeBase64(handshakeTrustResponseDTO.publicKey());
        String fingerprint = CryptoUtils.getFingerPrint(publicKey);

        String fingerprintExpected = CryptoUtils.getFingerPrint(CryptoUtils.decodeBase64(publicKeyExpectedBase64));
        if (!fingerprint.equals(fingerprintExpected)) {
            return HandshakeApiRequestResponseStatus.FINGERPRINT_MISMATCH;
        }

        String challenge = UUID.randomUUID().toString();
        HttpResponse<byte[]> challengeResponse = handshakeClient
                .getChallenge(nodeAddressURI, challenge);
        if (challengeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected challenge response code " + challengeResponse.statusCode());
        }

        HandshakeChallengeResponseDTO challengeResponseDTO = objectMapper
                .readValue(challengeResponse.body(), HandshakeChallengeResponseDTO.class);
        String signatureBase64 = challengeResponseDTO.signature();
        byte[] signature = CryptoUtils.decodeBase64(signatureBase64);
        boolean verify = CryptoUtils.verify(challenge.getBytes(), signature, publicKey);

        if (!verify) {
            handshakeStore.outgoingRequestStore().remove(fingerprint);
            handshakeStore.trustedOutStore().remove(fingerprint);
            return HandshakeApiRequestResponseStatus.CHALLENGE_FAILED;
        }
        byte[] encryptMessage = CryptoUtils.decodeBase64(handshakeTrustResponseDTO.encryptMessage());
        byte[] decryptMessage = CryptoUtils.decrypt(keysConfigStore.getRequired().privateKey(), encryptMessage);
        handshakeStore.outgoingRequestStore().remove(fingerprint);
        handshakeStore.trustedOutStore()
                .save(
                        fingerprint,
                        new TrustedOutKeyValueStore.TrustedOutValue(
                                nodeAddressURI,
                                publicKey,
                                new String(decryptMessage),
                                Instant.now()
                        )
                );
        return HandshakeApiRequestResponseStatus.SUCCESS;
    }

    public Optional<HandshakeApiOutgoingResponseDTO> getOutgoingRequest(String fingerprint) {
        handshakeStore.outgoingRequestStore().get(fingerprint)
                .map(it -> getOutgoingRequest(fingerprint, it));

        OutgoingRequestKeyValueStore.OutgoingRequestValue value = handshakeStore
                .outgoingRequestStore()
                .get(fingerprint)
                .orElse(null);
        if (value != null) {
            URI addressURI = value.addressURI();
            byte[] publicKey = value.publicKey();
            String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
            return Optional.of(new HandshakeApiOutgoingResponseDTO(fingerprint, publicKeyBase64, addressURI.toString()));
        }
        return Optional.empty();
    }

    public Optional<HandshakeApiTrustOutResponseDTO> getTrustOut(String fingerprint) {
        TrustedOutKeyValueStore.TrustedOutValue value = handshakeStore.trustedOutStore()
                .get(fingerprint)
                .orElse(null);
        if (value != null) {
            URI addressURI = value.addressURI();
            byte[] publicKey = value.publicKey();
            String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
            return Optional.of(new HandshakeApiTrustOutResponseDTO(fingerprint, publicKeyBase64, addressURI.toString()));
        }
        return Optional.empty();
    }

    private HandshakeApiTrustOutResponseDTO getTrustOut(String fingerprint, TrustedOutKeyValueStore.TrustedOutValue value) {
        String publicKeyBase64 = CryptoUtils.encodeBase64(value.publicKey());
        String addressURI = value.addressURI().toString();
        return new HandshakeApiTrustOutResponseDTO(fingerprint, publicKeyBase64, addressURI);
    }

    private HandshakeApiOutgoingResponseDTO getOutgoingRequest(String fingerprint, OutgoingRequestKeyValueStore.OutgoingRequestValue value) {
        String publicKeyBase64 = CryptoUtils.encodeBase64(value.publicKey());
        String addressURI = value.addressURI().toString();
        return new HandshakeApiOutgoingResponseDTO(fingerprint, publicKeyBase64, addressURI);
    }
}
