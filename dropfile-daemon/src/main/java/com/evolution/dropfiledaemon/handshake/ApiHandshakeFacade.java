package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigStore;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfigStore;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.exception.NoDaemonPublicAddressException;
import com.evolution.dropfiledaemon.handshake.exception.NoIncomingRequestFoundException;
import com.evolution.dropfiledaemon.handshake.store.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final DropFileKeysConfigStore keysConfigStore;

    private final DropFileAppConfigStore appConfigStore;

    public ApiHandshakeFacade(HandshakeStore handshakeStore,
                              HandshakeClient handshakeClient,
                              ObjectMapper objectMapper,
                              DropFileKeysConfigStore keysConfigStore,
                              DropFileAppConfigStore appConfigStore) {
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
            return doHandshakeChallenge(nodeAddressURI, handshakeResponse);
        } else if (handshakeResponse.statusCode() == 404) {
            return doHandshakeRequest(nodeAddressURI);
        }
        throw new RuntimeException(
                "Unexpected handshake trust request response code  " + handshakeResponse.statusCode()
        );
    }

    @SneakyThrows
    private HandshakeApiRequestResponseStatus doHandshakeRequest(URI nodeAddressURI) {
        DropFileAppConfig.DropFileDaemonAppConfig daemonAppConfig = appConfigStore.getRequired().daemonAppConfig();

        URI publicDaemonAddressURI = daemonAppConfig.publicDaemonAddressURI();
        if (publicDaemonAddressURI == null) {
            throw new NoDaemonPublicAddressException();
        }

        HttpResponse<byte[]> httpResponse = handshakeClient.handshakeRequest(
                publicDaemonAddressURI,
                nodeAddressURI,
                keysConfigStore.getRequired().publicKey()
        );
        if (httpResponse.statusCode() == 200) {
            HandshakeRequestResponseDTO responseDTO = objectMapper
                    .readValue(httpResponse.body(), HandshakeRequestResponseDTO.class);
            byte[] publicKeyBytes = CryptoUtils.decodeBase64(responseDTO.publicKey());
            String fingerPrint = CryptoUtils.getFingerPrint(publicKeyBytes);
            handshakeStore.outgoingRequestStore().save(
                    fingerPrint,
                    new OutgoingRequestKeyValueStore.OutgoingRequestValue(
                            nodeAddressURI,
                            publicKeyBytes
                    )
            );
            return HandshakeApiRequestResponseStatus.PENDING;
        }
        throw new RuntimeException("Unexpected handshake request response code " + httpResponse.statusCode());
    }

    @SneakyThrows
    private HandshakeApiRequestResponseStatus doHandshakeChallenge(URI nodeAddressURI, HttpResponse<byte[]> handshakeResponse) {
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
        HandshakeTrustResponseDTO handshakeTrustResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeTrustResponseDTO.class);
        byte[] publicKey = CryptoUtils.decodeBase64(handshakeTrustResponseDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);
        boolean verify = CryptoUtils.verify(challenge.getBytes(), signature, publicKey);
        if (!verify) {
            handshakeStore.outgoingRequestStore().remove(fingerPrint);
            handshakeStore.trustedOutStore().remove(fingerPrint);
            return HandshakeApiRequestResponseStatus.CHALLENGE_FAILED;
        }
        byte[] encryptMessage = CryptoUtils.decodeBase64(handshakeTrustResponseDTO.encryptMessage());
        byte[] decryptMessage = CryptoUtils.decrypt(keysConfigStore.getRequired().privateKey(), encryptMessage);
        handshakeStore.outgoingRequestStore().remove(fingerPrint);
        handshakeStore.trustedOutStore()
                .save(
                        fingerPrint,
                        new TrustedOutKeyValueStore.TrustedOutValue(
                                nodeAddressURI,
                                publicKey,
                                new String(decryptMessage),
                                Instant.now()
                        )
                );
        return HandshakeApiRequestResponseStatus.SUCCESS;
    }
}
