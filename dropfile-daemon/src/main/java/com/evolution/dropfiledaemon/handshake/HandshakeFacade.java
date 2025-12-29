package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.exception.HandshakeAlreadyTrustedException;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.IncomingRequestKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class HandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    @Autowired
    public HandshakeFacade(HandshakeStore handshakeStore,
                           KeysConfigStore keysConfigStore) {
        this.handshakeStore = handshakeStore;
        this.keysConfigStore = keysConfigStore;
    }

    public HandshakeIdentityResponseDTO getHandshakeIdentity() {
        byte[] publicKey = keysConfigStore.getRequired().publicKey();
        return new HandshakeIdentityResponseDTO(
                CryptoUtils.encodeBase64(publicKey),
                CryptoUtils.getFingerprint(publicKey)
        );
    }

    public HandshakeRequestResponseDTO request(HandshakeRequestBodyDTO requestDTO) {
        byte[] publicKey = CryptoUtils.decodeBase64(requestDTO.publicKey());
        String fingerprint = CryptoUtils.getFingerprint(publicKey);
        Optional<TrustedInKeyValueStore.TrustedInValue> trustedInValue = handshakeStore
                .trustedInStore()
                .get(fingerprint);
        if (trustedInValue.isPresent()) {
            throw new HandshakeAlreadyTrustedException(fingerprint);
        }
        handshakeStore.incomingRequestStore().save(
                fingerprint,
                new IncomingRequestKeyValueStore.IncomingRequestValue(requestDTO.addressURI(), publicKey)
        );
        return new HandshakeRequestResponseDTO(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().publicKey())
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
                trustedInValue.secret().getBytes()
        );
        String publicKeyBase64 = CryptoUtils.encodeBase64(keysConfigStore.getRequired().publicKey());
        String encryptSecretBase64 = CryptoUtils.encodeBase64(encryptSecret);
        return Optional.of(new HandshakeTrustResponseDTO(publicKeyBase64, encryptSecretBase64));
    }

    public HandshakeChallengeResponseDTO challenge(HandshakeChallengeRequestBodyDTO requestDTO) {
        byte[] signature = CryptoUtils.sign(
                requestDTO.challenge(),
                keysConfigStore.getRequired().privateKey()
        );
        String signatureBase64 = CryptoUtils.encodeBase64(signature);
        return new HandshakeChallengeResponseDTO(signatureBase64);
    }
}
