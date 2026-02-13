package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class HandshakeFacade {

    private static final int MAX_HANDSHAKE_TIMEOUT = 30_000;

    private final ApplicationConfigStore applicationConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public HandshakeResponseDTO handshake(HandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();
        Map.Entry<String, AccessKey> accessKey = applicationConfigStore.getAccessKeyStore().get(accessKeyId).orElse(null);
        if (accessKey != null) {
            return handshakeBasedOnSecret(requestDTO, accessKey.getValue());
        }
        Map.Entry<String, TrustedInKeyValueStore.TrustedInValue> trustedInValue = applicationConfigStore.getHandshakeStore().trustedInStore()
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
                CryptoECDH.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedIn.publicKeyDH())
        );
        SecretKey secretKey = cryptoTunnel.secretKey(secret);
        byte[] decryptMessage = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        HandshakeRequestDTO.Payload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.Payload.class);

        if (Math.abs(System.currentTimeMillis() - requestPayload.timestamp()) > MAX_HANDSHAKE_TIMEOUT) {
            throw new RuntimeException("Timed out");
        }

        boolean verify = CryptoRSA.verify(
                decryptMessage,
                requestDTO.signature(),
                CryptoRSA.getPublicKey(trustedIn.publicKeyRSA())
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        String fingerprint = requestDTO.id();
        if (!CommonUtils.getFingerprint(trustedIn.publicKeyRSA()).equals(fingerprint)) {
            throw new RuntimeException("Fingerprint mismatch: " + fingerprint);
        }

        HandshakeResponseDTO.Payload responsePayload = new HandshakeResponseDTO.Payload(
                applicationConfigStore.getKeysConfigStore().getRequired().rsa().publicKey(),
                applicationConfigStore.getKeysConfigStore().getRequired().dh().publicKey(),
                cryptoTunnel.getAlgorithm(),
                System.currentTimeMillis()
        );
        byte[] responsePayloadByteArray = objectMapper.writeValueAsBytes(responsePayload);
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                responsePayloadByteArray,
                secretKey
        );
        byte[] signature = CryptoRSA.sign(
                responsePayloadByteArray,
                CryptoRSA.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().rsa().privateKey())
        );

        return new HandshakeResponseDTO(
                secureEnvelope.payload(),
                secureEnvelope.nonce(),
                signature
        );
    }

    @SneakyThrows
    private HandshakeResponseDTO handshakeBasedOnSecret(HandshakeRequestDTO requestDTO, AccessKey accessKey) {
        applicationConfigStore.getAccessKeyStore().remove(requestDTO.id());

        SecretKey secretKey = cryptoTunnel.secretKey(accessKey.key().getBytes());
        byte[] decryptMessage = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        HandshakeRequestDTO.Payload requestPayload = objectMapper
                .readValue(decryptMessage, HandshakeRequestDTO.Payload.class);

        if (Math.abs(System.currentTimeMillis() - requestPayload.timestamp()) > MAX_HANDSHAKE_TIMEOUT) {
            throw new RuntimeException("Timed out");
        }

        boolean verify = CryptoRSA.verify(
                decryptMessage,
                requestDTO.signature(),
                CryptoRSA.getPublicKey(requestPayload.publicKeyRSA())
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        byte[] publicKeyRSA = requestPayload.publicKeyRSA();
        byte[] publicKeyDH = requestPayload.publicKeyDH();

        HandshakeResponseDTO.Payload responsePayload = new HandshakeResponseDTO.Payload(
                applicationConfigStore.getKeysConfigStore().getRequired().rsa().publicKey(),
                applicationConfigStore.getKeysConfigStore().getRequired().dh().publicKey(),
                cryptoTunnel.getAlgorithm(),
                System.currentTimeMillis()
        );
        byte[] responsePayloadByteArray = objectMapper.writeValueAsBytes(responsePayload);
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                responsePayloadByteArray,
                secretKey
        );

        byte[] signature = CryptoRSA.sign(
                responsePayloadByteArray,
                CryptoRSA.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().rsa().privateKey())
        );

        HandshakeResponseDTO handshakeResponseDTO = new HandshakeResponseDTO(
                secureEnvelope.payload(),
                secureEnvelope.nonce(),
                signature
        );

        applicationConfigStore.getHandshakeStore().trustedInStore()
                .save(
                        CommonUtils.getFingerprint(publicKeyRSA),
                        new TrustedInKeyValueStore.TrustedInValue(
                                publicKeyRSA,
                                publicKeyDH,
                                Instant.now()
                        )
                );

        return handshakeResponseDTO;
    }
}
