package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.*;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.configuration.DropFileKeyPairProvider;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class HandshakeFacade {

    private final HandshakeStoreManager handshakeManager;

    private final HandshakeClient handshakeClient;

    private final DropFileKeyPairProvider keyPairProvider;

    private final ObjectMapper objectMapper;

    @Autowired
    public HandshakeFacade(HandshakeStoreManager handshakeManager,
                           HandshakeClient handshakeClient,
                           DropFileKeyPairProvider keyPairProvider,
                           ObjectMapper objectMapper) {
        this.handshakeManager = handshakeManager;
        this.handshakeClient = handshakeClient;
        this.keyPairProvider = keyPairProvider;
        this.objectMapper = objectMapper;
    }

    public List<HandshakeStatusInfoDTO> getRequests() {
        return handshakeManager.getRequests()
                .stream()
                .map(it -> {
                    String publicKeyBase64 = Base64.getEncoder().encodeToString(it.publicKey());
                    return new HandshakeStatusInfoDTO(it.fingerprint(), publicKeyBase64);
                })
                .toList();
    }

    public List<HandshakeStatusInfoDTO> getTrusts() {
        return handshakeManager.getTrusts()
                .stream()
                .map(it -> {
                    String publicKeyBase64 = Base64.getEncoder().encodeToString(it.publicKey());
                    return new HandshakeStatusInfoDTO(it.fingerprint(), publicKeyBase64);
                })
                .toList();
    }

    @SneakyThrows
    private HandshakeApiRequestStatus handleHandshakeTrust(URI nodeAddressURI, HttpResponse<byte[]> handshakeResponse) {
        HandshakeTrustDTO handshakeTrustDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeTrustDTO.class);
        byte[] publicKey = Base64.getDecoder().decode(handshakeTrustDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        if (handshakeManager.getTrust(fingerPrint).isPresent()) {
            return HandshakeApiRequestStatus.SUCCESS;
        }

        String challenge = UUID.randomUUID().toString();
        HttpResponse<byte[]> challengeResponse = handshakeClient
                .getChallenge(nodeAddressURI, challenge);
        HandshakeChallengeResponseDTO challengeResponseDTO = objectMapper
                .readValue(challengeResponse.body(), HandshakeChallengeResponseDTO.class);

        boolean verify = CryptoUtils.verify(challenge, challengeResponseDTO.signature(), publicKey);
        if (!verify) {
            return HandshakeApiRequestStatus.CHALLENGE_FAILED;
        }
        byte[] encryptMessage = Base64.getDecoder().decode(handshakeTrustDTO.encryptMessage());
        byte[] decryptMessage = CryptoUtils.decrypt(keyPairProvider.getKeyPair().getPrivate(), encryptMessage);
        handshakeManager.putTrust(
                fingerPrint,
                publicKey,
                decryptMessage
        );
        return HandshakeApiRequestStatus.SUCCESS;
    }

    @SneakyThrows
    public HandshakeApiRequestStatus initializeRequest(HandshakeApiRequestDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.nodeAddress());
        PublicKey currentPublicKey = keyPairProvider.getKeyPair().getPublic();
        String currentFingerPrint = CryptoUtils.getFingerPrint(currentPublicKey);
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            return handleHandshakeTrust(nodeAddressURI, handshakeResponse);
        } else if (handshakeResponse.statusCode() == 404) {
            return handshakeRequest(nodeAddressURI);
        }
        throw new RuntimeException();
    }

    private HandshakeApiRequestStatus handshakeRequest(URI nodeAddressURI) {
        HttpResponse<Void> httpResponse = handshakeClient.handshakeRequest(
                nodeAddressURI,
                keyPairProvider.getKeyPair().getPublic().getEncoded()
        );
        if (httpResponse.statusCode() == 200) {
            return HandshakeApiRequestStatus.PENDING;
        }
        throw new RuntimeException();
    }

    public void request(HandshakeRequestDTO requestDTO) {
        byte[] publicKey = Base64.getDecoder().decode(requestDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        Optional<HandshakeStoreManager.HandshakeRequestValue> requestValue = handshakeManager.getRequest(fingerPrint);
        if (requestValue.isPresent()) {
            return;
        }
        Optional<HandshakeStoreManager.HandshakeTrustValue> trustValue = handshakeManager.getTrust(fingerPrint);
        if (trustValue.isPresent()) {
            return;
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
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPairProvider.getKeyPair().getPublic().getEncoded());
        String encryptSecretBase64 = Base64.getEncoder().encodeToString(encryptSecret);
        return Optional.of(new HandshakeTrustDTO(publicKeyBase64, encryptSecretBase64));
    }

    public void trust(String fingerprint) {
        byte[] secret = UUID.randomUUID().toString().getBytes();
        HandshakeStoreManager.HandshakeRequestValue requestValue = handshakeManager
                .getRequest(fingerprint)
                .orElseThrow();
        handshakeManager.putTrust(fingerprint, requestValue.publicKey(), secret);
    }

    public HandshakeChallengeResponseDTO challenge(HandshakeChallengeRequestDTO requestDTO) {
        KeyPair keyPair = keyPairProvider.getKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        String signature = CryptoUtils.sign(requestDTO.challenge(), privateKey);
        return new HandshakeChallengeResponseDTO(signature);
    }
}
