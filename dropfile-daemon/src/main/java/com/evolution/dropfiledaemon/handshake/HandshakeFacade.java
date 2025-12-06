package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.exception.HandshakeRequestAlreadyTrustedException;
import com.evolution.dropfiledaemon.handshake.store.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.UUID;

@Component
public class HandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final HandshakeClient handshakeClient;

    private final DropFileKeysConfig keysConfig;

    private final ObjectMapper objectMapper;

    private final ObjectProvider<DropFileAppConfig.DropFileDaemonAppConfig> daemonAppConfig;

    public HandshakeFacade(HandshakeStore handshakeStore,
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

    @SneakyThrows
    public HandshakeApiRequestStatus initializeRequest(HandshakeApiRequestDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.nodeAddress());
        String currentFingerPrint = CryptoUtils.getFingerPrint(keysConfig.getKeyPair().getPublic());
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            return handleHandshakeTrust(nodeAddressURI, handshakeResponse);
        } else if (handshakeResponse.statusCode() == 404) {
            return handshakeRequest(nodeAddressURI);
        }
        throw new RuntimeException(
                "Unexpected handshake trust request response code  " + handshakeResponse.statusCode()
        );
    }

    public HandshakeRequestResponseDTO request(HandshakeRequestDTO requestDTO) {
        byte[] publicKey = CryptoUtils.decodeBase64(requestDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);
        Optional<AllowedInKeyValueStore.AllowedInValue> allowedInValue = handshakeStore
                .allowedInStore()
                .get(fingerPrint);
        if (allowedInValue.isPresent()) {
            throw new HandshakeRequestAlreadyTrustedException();
        }
        handshakeStore.incomingRequestStore().save(
                fingerPrint,
                new IncomingRequestKeyValueStore.IncomingRequestValue(requestDTO.addressURI(), publicKey)
        );
        return new HandshakeRequestResponseDTO(
                CryptoUtils.encodeBase64(keysConfig.getKeyPair().getPublic().getEncoded())
        );
    }

    public Optional<HandshakeTrustDTO> getHandshakeApprove(String fingerprint) {
        AllowedInKeyValueStore.AllowedInValue allowedInValue = handshakeStore.allowedInStore()
                .get(fingerprint)
                .orElse(null);
        if (allowedInValue == null) {
            return Optional.empty();
        }
        byte[] encryptSecret = CryptoUtils.encrypt(
                allowedInValue.publicKey(),
                allowedInValue.secret()
        );
        String publicKeyBase64 = CryptoUtils.encodeBase64(keysConfig.getKeyPair().getPublic().getEncoded());
        String encryptSecretBase64 = CryptoUtils.encodeBase64(encryptSecret);
        return Optional.of(new HandshakeTrustDTO(publicKeyBase64, encryptSecretBase64));
    }

    public void trust(String fingerprint) {
        IncomingRequestKeyValueStore.IncomingRequestValue incomingRequestValue = handshakeStore
                .incomingRequestStore()
                .get(fingerprint)
                .orElseThrow();
        handshakeStore.incomingRequestStore().remove(fingerprint);
        byte[] secret = UUID.randomUUID().toString().getBytes();
        handshakeStore.allowedInStore()
                .save(
                        fingerprint,
                        new AllowedInKeyValueStore.AllowedInValue(
                                incomingRequestValue.addressURI(),
                                incomingRequestValue.publicKey(),
                                secret
                        )
                );
    }

    public HandshakeChallengeResponseDTO challenge(HandshakeChallengeRequestDTO requestDTO) {
        PrivateKey privateKey = keysConfig.getKeyPair().getPrivate();
        byte[] signature = CryptoUtils.sign(requestDTO.challenge(), privateKey);
        String signatureBase64 = CryptoUtils.encodeBase64(signature);
        return new HandshakeChallengeResponseDTO(signatureBase64);
    }

    @SneakyThrows
    private HandshakeApiRequestStatus handleHandshakeTrust(URI nodeAddressURI, HttpResponse<byte[]> handshakeResponse) {
        HandshakeTrustDTO handshakeTrustDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeTrustDTO.class);
        byte[] publicKey = CryptoUtils.decodeBase64(handshakeTrustDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        Optional<AllowedOutKeyValueStore.AllowedOutValue> allowedOutValue = handshakeStore.allowedOutStore()
                .get(fingerPrint);
        if (allowedOutValue.isPresent()) {
            return HandshakeApiRequestStatus.SUCCESS;
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
            return HandshakeApiRequestStatus.CHALLENGE_FAILED;
        }
        byte[] encryptMessage = CryptoUtils.decodeBase64(handshakeTrustDTO.encryptMessage());
        byte[] decryptMessage = CryptoUtils.decrypt(keysConfig.getKeyPair().getPrivate(), encryptMessage);
        handshakeStore.outgoingRequestStore().remove(fingerPrint);
        handshakeStore.allowedOutStore()
                .save(
                        fingerPrint,
                        new AllowedOutKeyValueStore.AllowedOutValue(nodeAddressURI, publicKey, decryptMessage)
                );
        return HandshakeApiRequestStatus.SUCCESS;
    }

    @SneakyThrows
    private HandshakeApiRequestStatus handshakeRequest(URI nodeAddressURI) {
        URI publicDaemonAddressURI = daemonAppConfig.getObject().getPublicDaemonAddressURI();
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
            int count = 30;
            while (count != 0) {
                System.out.println("WAITING APPROVE");
                String currentFingerPrint = CryptoUtils.getFingerPrint(keysConfig.getKeyPair().getPublic());
                HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
                if (handshakeResponse.statusCode() == 200) {
                    handleHandshakeTrust(nodeAddressURI, handshakeResponse);
                    return HandshakeApiRequestStatus.SUCCESS;
                }
                Thread.sleep(1000);
                count--;
            }

            return HandshakeApiRequestStatus.PENDING;
        }
        throw new RuntimeException("Unexpected handshake request response code " + httpResponse.statusCode());
    }
}
