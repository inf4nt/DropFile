package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.crypto.CryptoTunnelChaCha;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.IncomingRequestKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Optional;

@Slf4j
@Component
public class HandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final ObjectMapper objectMapper;

    @Autowired
    public HandshakeFacade(HandshakeStore handshakeStore,
                           KeysConfigStore keysConfigStore,
                           ObjectMapper objectMapper) {
        this.handshakeStore = handshakeStore;
        this.keysConfigStore = keysConfigStore;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public HandshakeIdentityResponseDTO getHandshakeIdentity() {
        HandshakeIdentityResponseDTO.HandshakeIdentityPayload payload = new HandshakeIdentityResponseDTO.HandshakeIdentityPayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey())
        );

        byte[] signature = CryptoUtils.sign(
                objectMapper.writeValueAsBytes(payload),
                CryptoUtils.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey())
        );

        return new HandshakeIdentityResponseDTO(
                payload,
                CryptoUtils.encodeBase64(signature)
        );
    }

    @SneakyThrows
    public HandshakeResponseDTO doHandshake(HandshakeRequestDTO requestDTO) {
        HandshakeRequestDTO.HandshakePayload payload = requestDTO.payload();

        String fingerprint = CryptoUtils.getFingerprint(CryptoUtils.decodeBase64(
                payload.publicKeyRSA()
        ));

        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore
                .trustedInStore()
                .get(fingerprint)
                .orElse(null);
        if (trustedInValue == null) {
            handshakeStore.incomingRequestStore()
                    .save(
                            fingerprint,
                            new IncomingRequestKeyValueStore.IncomingRequestValue(
                                    CryptoUtils.decodeBase64(payload.publicKeyRSA()),
                                    CryptoUtils.decodeBase64(payload.publicKeyDH())
                            )
                    );
        }

        HandshakeResponseDTO.HandshakePayload payloadResponse = new HandshakeResponseDTO.HandshakePayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                System.currentTimeMillis()
        );
        byte[] signature = CryptoUtils.sign(
                objectMapper.writeValueAsBytes(payloadResponse),
                CryptoUtils.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey())
        );

        return new HandshakeResponseDTO(
                payloadResponse,
                CryptoUtils.encodeBase64(signature)
        );
    }

    @SneakyThrows
    public void ping(PingRequestDTO requestDTO) {
        String fingerprint = requestDTO.fingerprint();
        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore.trustedInStore()
                .get(fingerprint)
                .orElse(null);

        if (trustedInValue == null) {
            throw new RuntimeException("No trusted fingerprint found");
        }

        byte[] secret = CryptoUtils.deriveSharedSecret(
                CryptoUtils.getPrivateKeyDH(keysConfigStore.getRequired().dh().privateKey()),
                CryptoUtils.getPublicKeyDH(trustedInValue.publicKeyDH())
        );

        SecretKey secretKey = new SecretKeySpec(
                MessageDigest
                        .getInstance("SHA-256")
                        .digest(secret),
                "ChaCha20"
        );

        byte[] decryptPayload = CryptoTunnelChaCha
                .decrypt(requestDTO.payload(), requestDTO.nonce(), secretKey);

        PingRequestDTO.PingRequestPayload pingRequestPayload = objectMapper
                .readValue(decryptPayload, PingRequestDTO.PingRequestPayload.class);

        if (Math.abs(System.currentTimeMillis() - pingRequestPayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }
    }


    @Deprecated
    public HandshakeRequestResponseDTO request(HandshakeRequestBodyDTO requestDTO) {
//        byte[] publicKey = CryptoUtils.decodeBase64(requestDTO.publicKey());
//        String fingerprint = CryptoUtils.getFingerprint(publicKey);
//        Optional<TrustedInKeyValueStore.TrustedInValue> trustedInValue = handshakeStore
//                .trustedInStore()
//                .get(fingerprint);
//        if (trustedInValue.isPresent()) {
//            throw new HandshakeAlreadyTrustedException(fingerprint);
//        }
//        handshakeStore.incomingRequestStore().save(
//                fingerprint,
//                new IncomingRequestKeyValueStore.IncomingRequestValue(requestDTO.addressURI(), publicKey)
//        );
//        return new HandshakeRequestResponseDTO(
//                CryptoUtils.encodeBase64(keysConfigStore.getRequired().publicKey())
//        );

        throw new RuntimeException();
    }

    public Optional<HandshakeTrustResponseDTO> getHandshakeApprove(String fingerprint) {
//        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore.trustedInStore()
//                .get(fingerprint)
//                .orElse(null);
//        if (trustedInValue == null) {
//            return Optional.empty();
//        }
//        byte[] encryptSecret = CryptoUtils.encrypt(
//                trustedInValue.publicKey(),
//                trustedInValue.secret().getBytes()
//        );
//        String publicKeyBase64 = CryptoUtils.encodeBase64(keysConfigStore.getRequired().publicKey());
//        String encryptSecretBase64 = CryptoUtils.encodeBase64(encryptSecret);
//        return Optional.of(new HandshakeTrustResponseDTO(publicKeyBase64, encryptSecretBase64));

        throw new RuntimeException();
    }

    public HandshakeChallengeResponseDTO challenge(HandshakeChallengeRequestBodyDTO requestDTO) {
//        byte[] signature = CryptoUtils.sign(
//                requestDTO.challenge(),
//                CryptoUtils.getPrivateKey(keysConfigStore.getRequired().privateKey())
//        );
//        String signatureBase64 = CryptoUtils.encodeBase64(signature);
//        return new HandshakeChallengeResponseDTO(signatureBase64);

        throw new RuntimeException();
    }
}
