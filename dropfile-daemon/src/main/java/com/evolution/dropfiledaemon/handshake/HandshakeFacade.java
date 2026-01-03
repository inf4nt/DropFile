package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.crypto.*;
import com.evolution.dropfile.common.dto.HandshakeIdentityResponseDTO;
import com.evolution.dropfile.common.dto.HandshakeRequestDTO;
import com.evolution.dropfile.common.dto.HandshakeResponseDTO;
import com.evolution.dropfile.configuration.access.AccessKey;
import com.evolution.dropfile.configuration.access.AccessKeyStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class HandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final AccessKeyStore accessKeyStore;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    public HandshakeFacade(HandshakeStore handshakeStore,
                           KeysConfigStore keysConfigStore,
                           AccessKeyStore accessKeyStore,
                           CryptoTunnel cryptoTunnel,
                           ObjectMapper objectMapper) {
        this.handshakeStore = handshakeStore;
        this.keysConfigStore = keysConfigStore;
        this.accessKeyStore = accessKeyStore;
        this.cryptoTunnel = cryptoTunnel;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public HandshakeIdentityResponseDTO getHandshakeIdentity() {
        HandshakeIdentityResponseDTO.HandshakeIdentityPayload payload = new HandshakeIdentityResponseDTO.HandshakeIdentityPayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey())
        );

        byte[] signature = CryptoRSA.sign(
                objectMapper.writeValueAsBytes(payload),
                CryptoRSA.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey())
        );

        return new HandshakeIdentityResponseDTO(
                payload,
                CryptoUtils.encodeBase64(signature)
        );
    }

    @SneakyThrows
    public HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();
        AccessKey accessKey = accessKeyStore.get(accessKeyId).orElse(null);
        if (accessKey != null) {
            return handshakeBasedOnSecret(requestDTO, accessKey);
        }
        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore.trustedInStore()
                .get(accessKeyId)
                .orElse(null);
        if (trustedInValue != null) {
            return handshakeBasedOnFingerprint(requestDTO, trustedInValue);
        }
        throw new RuntimeException("No access key or trusted-in connections found: " + accessKeyId);
    }

    @SneakyThrows
    private HandshakeResponseDTO handshakeBasedOnFingerprint(HandshakeRequestDTO requestDTO, TrustedInKeyValueStore.TrustedInValue trustedIn) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedIn.publicKeyDH())
        );
        SecretKey secretKey = cryptoTunnel.secretKey(secret);
        byte[] decryptMessage = cryptoTunnel.decrypt(
                CryptoUtils.decodeBase64(requestDTO.payload()),
                CryptoUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        HandshakeRequestDTO.HandshakePayload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.HandshakePayload.class);

        String fingerprint = requestDTO.id();
        if (!CryptoUtils.getFingerprint(trustedIn.publicKeyDH()).equals(fingerprint)) {
            throw new RuntimeException("Fingerprint mismatch: " + fingerprint);
        }

        if (Math.abs(System.currentTimeMillis() - requestPayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        HandshakeResponseDTO.HandshakePayload responsePayload = new HandshakeResponseDTO.HandshakePayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                HandshakeResponseDTO.HandshakeStatus.OK,
                cryptoTunnel.getAlgorithm(),
                System.currentTimeMillis()
        );
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(responsePayload),
                secretKey
        );

        return new HandshakeResponseDTO(
                CryptoUtils.encodeBase64(secureEnvelope.payload()),
                CryptoUtils.encodeBase64(secureEnvelope.nonce())
        );
    }

    @SneakyThrows
    private HandshakeResponseDTO handshakeBasedOnSecret(HandshakeRequestDTO requestDTO, AccessKey accessKey) {
        String accessKeyId = requestDTO.id();
        SecretKey secretKey = cryptoTunnel.secretKey(accessKey.key().getBytes());
        byte[] decryptMessage = cryptoTunnel.decrypt(
                CryptoUtils.decodeBase64(requestDTO.payload()),
                CryptoUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        HandshakeRequestDTO.HandshakePayload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.HandshakePayload.class);

        if (Math.abs(System.currentTimeMillis() - requestPayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        byte[] publicKeyDH = CryptoUtils.decodeBase64(requestPayload.publicKeyDH());
        handshakeStore.trustedInStore()
                .save(
                        CryptoUtils.getFingerprint(publicKeyDH),
                        new TrustedInKeyValueStore.TrustedInValue(
                                publicKeyDH
                        )
                );

        HandshakeResponseDTO.HandshakePayload responsePayload = new HandshakeResponseDTO.HandshakePayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                HandshakeResponseDTO.HandshakeStatus.OK,
                cryptoTunnel.getAlgorithm(),
                System.currentTimeMillis()
        );
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(responsePayload),
                secretKey
        );

        accessKeyStore.remove(accessKeyId);

        return new HandshakeResponseDTO(
                CryptoUtils.encodeBase64(secureEnvelope.payload()),
                CryptoUtils.encodeBase64(secureEnvelope.nonce())
        );
    }
}
