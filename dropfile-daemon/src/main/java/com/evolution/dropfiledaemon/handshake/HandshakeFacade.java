package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.dto.HandshakeApiRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeApproveDTO;
import com.evolution.dropfile.configuration.dto.HandshakeInfoDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.configuration.DropFileKeyPairProvider;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStoreManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

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

    public Map<String, List<HandshakeInfoDTO>> getStatus() {
        return new LinkedHashMap<>() {{
            List<HandshakeInfoDTO> requests = handshakeManager.getRequests().stream()
                    .map(it -> new HandshakeInfoDTO(it.fingerprint(), it.publicKey())).toList();
            List<HandshakeInfoDTO> trusts = handshakeManager.getTrusts().stream()
                    .map(it -> new HandshakeInfoDTO(it.fingerprint(), it.publicKey())).toList();
            put("requests", requests);
            put("trusts", trusts);
        }};
    }

    @SneakyThrows
    public void initializeRequest(HandshakeApiRequestDTO requestBody) {
        URI nodeAddressURI = CommonUtils.toURI(requestBody.getNodeAddress());
        PublicKey currentPublicKey = keyPairProvider.getKeyPair().getPublic();
        String currentFingerPrint = CryptoUtils.getFingerPrint(currentPublicKey);
        HttpResponse<byte[]> handshakeResponse = handshakeClient.getHandshake(nodeAddressURI, currentFingerPrint);
        if (handshakeResponse.statusCode() == 200) {
            HandshakeApproveDTO handshakeApproveDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeApproveDTO.class);
            PrivateKey currentPrivateKey = keyPairProvider.getKeyPair().getPrivate();
            byte[] decryptMessage = CryptoUtils.decrypt(currentPrivateKey, handshakeApproveDTO.encryptMessage());

            String fingerPrint = CryptoUtils.getFingerPrint(handshakeApproveDTO.publicKey());
            handshakeManager.trust(
                    fingerPrint,
                    handshakeApproveDTO.publicKey(),
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

    public Optional<HandshakeApproveDTO> getHandshakeApprove(String fingerprint) {
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
        return Optional.of(new HandshakeApproveDTO(keyPairProvider.getKeyPair().getPublic().getEncoded(), encryptSecret));
    }

    public HandshakeApproveDTO approve(String fingerprint) {
        byte[] secret = UUID.randomUUID().toString().getBytes();
        handshakeManager.requestToTrust(fingerprint, secret);
        return new HandshakeApproveDTO(keyPairProvider.getKeyPair().getPublic().getEncoded(), secret);
    }
}
