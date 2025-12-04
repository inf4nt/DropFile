package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.HandshakeApiRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeTrustDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeStatusInfoDTO;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.configuration.DropFileKeyPairProvider;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class HandshakeFacade {

    private final HandshakeStoreManager handshakeManager;

    private final HandshakeClient handshakeClient;

    private final DropFileKeyPairProvider keyPairProvider;

    private final ObjectMapper objectMapper;
    private final HandlerMapping resourceHandlerMapping;

    @Autowired
    public HandshakeFacade(HandshakeStoreManager handshakeManager,
                           HandshakeClient handshakeClient,
                           DropFileKeyPairProvider keyPairProvider,
                           ObjectMapper objectMapper, HandlerMapping resourceHandlerMapping) {
        this.handshakeManager = handshakeManager;
        this.handshakeClient = handshakeClient;
        this.keyPairProvider = keyPairProvider;
        this.objectMapper = objectMapper;
        this.resourceHandlerMapping = resourceHandlerMapping;
    }

    public List<HandshakeStatusInfoDTO> getRequests() {
        return handshakeManager.getRequests()
                .stream()
                .map(it -> new HandshakeStatusInfoDTO(it.fingerprint(), it.publicKey()))
                .toList();
    }

    public List<HandshakeStatusInfoDTO> getTrusts() {
        return handshakeManager.getTrusts()
                .stream()
                .map(it -> new HandshakeStatusInfoDTO(it.fingerprint(), it.publicKey()))
                .toList();
    }

    @SneakyThrows
    public void initializeRequest(HandshakeApiRequestDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.getNodeAddress());
        PublicKey currentPublicKey = keyPairProvider.getKeyPair().getPublic();
        String currentFingerPrint = CryptoUtils.getFingerPrint(currentPublicKey);
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            HandshakeTrustDTO handshakeTrustDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeTrustDTO.class);
            PrivateKey currentPrivateKey = keyPairProvider.getKeyPair().getPrivate();
            byte[] decryptMessage = CryptoUtils.decrypt(currentPrivateKey, handshakeTrustDTO.encryptMessage());
            String fingerPrint = CryptoUtils.getFingerPrint(handshakeTrustDTO.publicKey());
            handshakeManager.trust(
                    fingerPrint,
                    handshakeTrustDTO.publicKey(),
                    decryptMessage
            );
        } else if (handshakeResponse.statusCode() == 404) {
            handshakeClient.handshakeRequest(
                    nodeAddressURI,
                    currentPublicKey
            );
        }
    }

    public void request(HandshakeRequestDTO requestDTO) {
        byte[] publicKeyBytes = requestDTO.publicKey();
        String fingerPrint = CryptoUtils.getFingerPrint(publicKeyBytes);
        Optional<HandshakeStoreManager.HandshakeRequestValue> requestValue = handshakeManager.getRequest(fingerPrint);
        if (requestValue.isPresent()) {
            return;
        }
        Optional<HandshakeStoreManager.HandshakeTrustValue> trustValue = handshakeManager.getTrust(fingerPrint);
        if (trustValue.isPresent()) {
            return;
        }
        handshakeManager.request(fingerPrint, publicKeyBytes);
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
        return Optional.of(new HandshakeTrustDTO(keyPairProvider.getKeyPair().getPublic().getEncoded(), encryptSecret));
    }

    public HandshakeTrustDTO trust(String fingerprint) {
        byte[] secret = UUID.randomUUID().toString().getBytes();
        handshakeManager.requestToTrust(fingerprint, secret);
        return new HandshakeTrustDTO(keyPairProvider.getKeyPair().getPublic().getEncoded(), secret);
    }
}
