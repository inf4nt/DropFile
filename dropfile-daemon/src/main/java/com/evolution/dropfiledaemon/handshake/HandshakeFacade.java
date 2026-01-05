package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.evolution.dropfile.common.dto.HandshakeRequestDTO;
import com.evolution.dropfile.common.dto.HandshakeResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Map;

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
    public HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();
        Map.Entry<String, AccessKey> accessKey = accessKeyStore.get(accessKeyId).orElse(null);
        if (accessKey != null) {
            return handshakeBasedOnSecret(requestDTO, accessKey.getValue());
        }
        Map.Entry<String, TrustedInKeyValueStore.TrustedInValue> trustedInValue = handshakeStore.trustedInStore()
                .get(accessKeyId)
                .orElse(null);
        if (trustedInValue != null) {
            return handshakeBasedOnFingerprint(requestDTO, trustedInValue.getValue());
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
                CommonUtils.decodeBase64(requestDTO.payload()),
                CommonUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        HandshakeRequestDTO.HandshakePayload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.HandshakePayload.class);

        boolean verify = CryptoRSA.verify(
                decryptMessage,
                CommonUtils.decodeBase64(requestDTO.signature()),
                CryptoRSA.getPublicKey(trustedIn.publicKeyRSA())
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        String fingerprint = requestDTO.id();
        if (!CommonUtils.getFingerprint(trustedIn.publicKeyRSA()).equals(fingerprint)) {
            throw new RuntimeException("Fingerprint mismatch: " + fingerprint);
        }

        if (Math.abs(System.currentTimeMillis() - requestPayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        HandshakeResponseDTO.HandshakePayload responsePayload = new HandshakeResponseDTO.HandshakePayload(
                CommonUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                HandshakeResponseDTO.HandshakeStatus.OK,
                cryptoTunnel.getAlgorithm(),
                System.currentTimeMillis()
        );
        byte[] responsePayloadByteArray = objectMapper.writeValueAsBytes(responsePayload);
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                responsePayloadByteArray,
                secretKey
        );
        byte[] signature = CryptoRSA.sign(responsePayloadByteArray, CryptoRSA.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey()));

        return new HandshakeResponseDTO(
                CommonUtils.encodeBase64(secureEnvelope.payload()),
                CommonUtils.encodeBase64(secureEnvelope.nonce()),
                CommonUtils.encodeBase64(signature)
        );
    }

    @SneakyThrows
    private HandshakeResponseDTO handshakeBasedOnSecret(HandshakeRequestDTO requestDTO, AccessKey accessKey) {
        SecretKey secretKey = cryptoTunnel.secretKey(accessKey.key().getBytes());
        byte[] decryptMessage = cryptoTunnel.decrypt(
                CommonUtils.decodeBase64(requestDTO.payload()),
                CommonUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        HandshakeRequestDTO.HandshakePayload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.HandshakePayload.class);

        if (Math.abs(System.currentTimeMillis() - requestPayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        boolean verify = CryptoRSA.verify(
                decryptMessage,
                CommonUtils.decodeBase64(requestDTO.signature()),
                CryptoRSA.getPublicKey(CommonUtils.decodeBase64(requestPayload.publicKeyRSA()))
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        byte[] publicKeyRSA = CommonUtils.decodeBase64(requestPayload.publicKeyRSA());
        byte[] publicKeyDH = CommonUtils.decodeBase64(requestPayload.publicKeyDH());
        handshakeStore.trustedInStore()
                .save(
                        CommonUtils.getFingerprint(publicKeyRSA),
                        new TrustedInKeyValueStore.TrustedInValue(
                                publicKeyRSA,
                                publicKeyDH
                        )
                );

        HandshakeResponseDTO.HandshakePayload responsePayload = new HandshakeResponseDTO.HandshakePayload(
                CommonUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                HandshakeResponseDTO.HandshakeStatus.OK,
                cryptoTunnel.getAlgorithm(),
                System.currentTimeMillis()
        );
        byte[] responsePayloadByteArray = objectMapper.writeValueAsBytes(responsePayload);
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                responsePayloadByteArray,
                secretKey
        );

        byte[] signature = CryptoRSA.sign(responsePayloadByteArray, CryptoRSA.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey()));

        accessKeyStore.remove(requestDTO.id());

        return new HandshakeResponseDTO(
                CommonUtils.encodeBase64(secureEnvelope.payload()),
                CommonUtils.encodeBase64(secureEnvelope.nonce()),
                CommonUtils.encodeBase64(signature)
        );
    }
}
