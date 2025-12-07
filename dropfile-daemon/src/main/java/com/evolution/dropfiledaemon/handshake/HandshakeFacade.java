package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.exception.HandshakeAlreadyTrustedException;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.IncomingRequestKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.util.Optional;

@Slf4j
@Component
public class HandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final HandshakeClient handshakeClient;

    private final DropFileKeysConfig keysConfig;

    private final ObjectMapper objectMapper;

    @Autowired
    public HandshakeFacade(HandshakeStore handshakeStore,
                           HandshakeClient handshakeClient,
                           DropFileKeysConfig keysConfig,
                           ObjectMapper objectMapper) {
        this.handshakeStore = handshakeStore;
        this.handshakeClient = handshakeClient;
        this.keysConfig = keysConfig;
        this.objectMapper = objectMapper;
    }


    public HandshakeRequestResponseDTO request(HandshakeRequestBodyDTO requestDTO) {
        byte[] publicKey = CryptoUtils.decodeBase64(requestDTO.publicKey());
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);
        Optional<TrustedInKeyValueStore.TrustedInValue> trustedInValue = handshakeStore
                .trustedInStore()
                .get(fingerPrint);
        if (trustedInValue.isPresent()) {
            throw new HandshakeAlreadyTrustedException(fingerPrint);
        }
        handshakeStore.incomingRequestStore().save(
                fingerPrint,
                new IncomingRequestKeyValueStore.IncomingRequestValue(requestDTO.addressURI(), publicKey)
        );
        return new HandshakeRequestResponseDTO(
                CryptoUtils.encodeBase64(keysConfig.getKeyPair().getPublic().getEncoded())
        );
    }

    public Optional<HandshakeTrustResponseDTO> getHandshakeApprove(String fingerprint) {
        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore.trustedInStore()
                .get(fingerprint)
                .orElse(null);
        if (trustedInValue == null) {
            return Optional.empty();
        }
        byte[] encryptSecret = CryptoUtils.encrypt(
                trustedInValue.publicKey(),
                trustedInValue.secret()
        );
        String publicKeyBase64 = CryptoUtils.encodeBase64(keysConfig.getKeyPair().getPublic().getEncoded());
        String encryptSecretBase64 = CryptoUtils.encodeBase64(encryptSecret);
        return Optional.of(new HandshakeTrustResponseDTO(publicKeyBase64, encryptSecretBase64));
    }

    public HandshakeChallengeResponseDTO challenge(HandshakeChallengeRequestBodyDTO requestDTO) {
        PrivateKey privateKey = keysConfig.getKeyPair().getPrivate();
        byte[] signature = CryptoUtils.sign(requestDTO.challenge(), privateKey);
        String signatureBase64 = CryptoUtils.encodeBase64(signature);
        return new HandshakeChallengeResponseDTO(signatureBase64);
    }
}
