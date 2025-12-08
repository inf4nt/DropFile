package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.exception.ApiHandshakeNoDaemonPublicAddressException;
import com.evolution.dropfiledaemon.handshake.exception.ApiHandshakeNoIncomingRequestFoundException;
import com.evolution.dropfiledaemon.handshake.store.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ApiHandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final HandshakeClient handshakeClient;

    private final DropFileKeysConfig keysConfig;

    private final ObjectMapper objectMapper;

    private final ObjectProvider<DropFileAppConfig.DropFileDaemonAppConfig> daemonAppConfig;

    @Autowired
    public ApiHandshakeFacade(HandshakeStore handshakeStore,
                              HandshakeClient handshakeClient,
                              DropFileKeysConfig keysConfig,
                              ObjectMapper objectMapper,
                              ObjectProvider<DropFileAppConfig.DropFileDaemonAppConfig> daemonAppConfig) {
        this.handshakeStore = handshakeStore;
        this.handshakeClient = handshakeClient;
        this.keysConfig = keysConfig;
        this.objectMapper = objectMapper;
        this.daemonAppConfig = daemonAppConfig;
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

    @SneakyThrows
    public HandshakeApiRequestResponseStatus initializeRequest(HandshakeApiRequestBodyDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.nodeAddress());
        String currentFingerPrint = CryptoUtils.getFingerPrint(keysConfig.getKeyPair().getPublic());
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            return handleHandshakeTrust(nodeAddressURI, handshakeResponse);
        } else if (handshakeResponse.statusCode() == 404) {
            return handshakeRequest(nodeAddressURI, requestBody.timeout());
        }
        throw new RuntimeException(
                "Unexpected handshake trust request response code  " + handshakeResponse.statusCode()
        );
    }

    public void trust(String fingerprint) {
        IncomingRequestKeyValueStore.IncomingRequestValue incomingRequestValue = handshakeStore
                .incomingRequestStore()
                .get(fingerprint)
                .orElseThrow(() -> new ApiHandshakeNoIncomingRequestFoundException(fingerprint));
        handshakeStore.incomingRequestStore().remove(fingerprint);
        String secret = UUID.randomUUID().toString();
        System.out.println("GENERATED SECRET KEY: " + secret);
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
    private HandshakeApiRequestResponseStatus handshakeRequest(URI nodeAddressURI, Integer timeout) {
        URI publicDaemonAddressURI = daemonAppConfig.getObject().getPublicDaemonAddressURI();
        if (publicDaemonAddressURI == null) {
            throw new ApiHandshakeNoDaemonPublicAddressException();
        }

        HttpResponse<byte[]> httpResponse = handshakeClient.handshakeRequest(
                publicDaemonAddressURI,
                nodeAddressURI,
                keysConfig.getKeyPair().getPublic().getEncoded()
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
            int count = 0;
            log.info("Waiting for handshake approval. Attempt timeout: {} ", timeout);
            while (count != timeout) {
                log.info("Waiting for handshake approval. Attempt: {}", count);
                String currentFingerPrint = CryptoUtils.getFingerPrint(keysConfig.getKeyPair().getPublic());
                HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
                if (handshakeResponse.statusCode() == 200) {
                    handleHandshakeTrust(nodeAddressURI, handshakeResponse);
                    log.info("Handshake approval completed");
                    return HandshakeApiRequestResponseStatus.SUCCESS;
                }
                Thread.sleep(1000);
                count++;
            }
            return HandshakeApiRequestResponseStatus.PENDING;
        }
        throw new RuntimeException("Unexpected handshake request response code " + httpResponse.statusCode());
    }

    @SneakyThrows
    private HandshakeApiRequestResponseStatus handleHandshakeTrust(URI nodeAddressURI, HttpResponse<byte[]> handshakeResponse) {
        HandshakeTrustResponseDTO handshakeTrustResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeTrustResponseDTO.class);
        byte[] publicKey = CryptoUtils.decodeBase64(handshakeTrustResponseDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        Optional<TrustedOutKeyValueStore.TrustedOutValue> allowedOutValue = handshakeStore.trustedOutStore()
                .get(fingerPrint);
        if (allowedOutValue.isPresent()) {
            return HandshakeApiRequestResponseStatus.SUCCESS;
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
            return HandshakeApiRequestResponseStatus.CHALLENGE_FAILED;
        }
        byte[] encryptMessage = CryptoUtils.decodeBase64(handshakeTrustResponseDTO.encryptMessage());
        byte[] decryptMessage = CryptoUtils.decrypt(keysConfig.getKeyPair().getPrivate(), encryptMessage);
        handshakeStore.outgoingRequestStore().remove(fingerPrint);
        handshakeStore.trustedOutStore()
                .save(
                        fingerPrint,
                        new TrustedOutKeyValueStore.TrustedOutValue(
                                nodeAddressURI,
                                publicKey,
                                new String(decryptMessage)
                        )
                );
        return HandshakeApiRequestResponseStatus.SUCCESS;
    }
}
