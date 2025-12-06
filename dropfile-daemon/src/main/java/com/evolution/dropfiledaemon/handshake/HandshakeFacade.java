package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.*;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.exception.HandshakeRequestAlreadyTrustedException;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class HandshakeFacade {

    private final HandshakeStoreManager handshakeManager;

    private final HandshakeClient handshakeClient;

    private final DropFileKeysConfig keysConfig;

    private final ObjectMapper objectMapper;

    public HandshakeFacade(HandshakeStoreManager handshakeManager,
                           HandshakeClient handshakeClient,
                           DropFileKeysConfig keysConfig,
                           ObjectMapper objectMapper) {
        this.handshakeManager = handshakeManager;
        this.handshakeClient = handshakeClient;
        this.keysConfig = keysConfig;
        this.objectMapper = objectMapper;
    }

    public List<HandshakeStatusInfoDTO> getRequests() {
        return handshakeManager.getRequests()
                .stream()
                .map(it -> {
                    String publicKeyBase64 = CryptoUtils.encodeBase64(it.publicKey());
                    return new HandshakeStatusInfoDTO(it.fingerprint(), publicKeyBase64);
                })
                .toList();
    }

    public List<HandshakeStatusInfoDTO> getTrusts() {
        return handshakeManager.getTrusts()
                .stream()
                .map(it -> {
                    String publicKeyBase64 = CryptoUtils.encodeBase64(it.publicKey());
                    return new HandshakeStatusInfoDTO(it.fingerprint(), publicKeyBase64);
                })
                .toList();
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

    public void request(HandshakeRequestDTO requestDTO) {
        byte[] publicKey = CryptoUtils.decodeBase64(requestDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);
        Optional<HandshakeStoreManager.HandshakeTrustValue> trustValue = handshakeManager.getTrust(fingerPrint);
        if (trustValue.isPresent()) {
            throw new HandshakeRequestAlreadyTrustedException();
        }
        handshakeManager.putRequest(fingerPrint, publicKey);
    }

    public Optional<HandshakeTrustDTO> getHandshakeApprove(String fingerprint) {
        HandshakeStoreManager.HandshakeTrustValue trustValue = handshakeManager
                .getTrust(fingerprint)
                .orElse(null);
        if (trustValue == null) {
            return Optional.empty();
        }
        byte[] encryptSecret = CryptoUtils.encrypt(
                trustValue.publicKey(),
                trustValue.secret()
        );
        String publicKeyBase64 = CryptoUtils.encodeBase64(keysConfig.getKeyPair().getPublic().getEncoded());
        String encryptSecretBase64 = CryptoUtils.encodeBase64(encryptSecret);
        return Optional.of(new HandshakeTrustDTO(publicKeyBase64, encryptSecretBase64));
    }

    public void trust(String fingerprint) {
        HandshakeStoreManager.HandshakeRequestValue requestValue = handshakeManager
                .getRequest(fingerprint)
                .orElseThrow();
        byte[] secret = UUID.randomUUID().toString().getBytes();
        handshakeManager.putTrust(fingerprint, requestValue.publicKey(), secret);
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

        if (handshakeManager.getTrust(fingerPrint).isPresent()) {
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
        handshakeManager.putTrust(
                fingerPrint,
                publicKey,
                decryptMessage
        );
        return HandshakeApiRequestStatus.SUCCESS;
    }

    private HandshakeApiRequestStatus handshakeRequest(URI nodeAddressURI) {
        HttpResponse<Void> httpResponse = handshakeClient.handshakeRequest(
                nodeAddressURI,
                keysConfig.getKeyPair().getPublic().getEncoded()
        );
        if (httpResponse.statusCode() == 200) {
            return HandshakeApiRequestStatus.PENDING;
        }
        throw new RuntimeException("Unexpected handshake request response code " + httpResponse.statusCode());
    }
}
