package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.DoHandshakeRequestDTO;
import com.evolution.dropfile.common.dto.DoHandshakeResponseDTO;
import com.evolution.dropfile.common.dto.HandshakeIdentityResponseDTO;
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
    public DoHandshakeResponseDTO handshake(DoHandshakeRequestDTO requestDTO) {
        String accessKeyId = requestDTO.id();
        AccessKey accessKey = accessKeyStore.get(accessKeyId).orElse(null);
        if (accessKey == null) {
            throw new RuntimeException("No access key found: " + accessKeyId);
        }
        SecretKey secretKey = cryptoTunnel.secretKey(accessKey.key().getBytes());
        byte[] decryptMessage = cryptoTunnel.decrypt(
                CryptoUtils.decodeBase64(requestDTO.payload()),
                CryptoUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        DoHandshakeRequestDTO.DoHandshakePayload requestPayload = objectMapper
                .readValue(decryptMessage, DoHandshakeRequestDTO.DoHandshakePayload.class);

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

        DoHandshakeResponseDTO.DoHandshakePayload responsePayload = new DoHandshakeResponseDTO.DoHandshakePayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                DoHandshakeResponseDTO.HandshakeStatus.APPROVED,
                System.currentTimeMillis()
        );
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(responsePayload),
                secretKey
        );

        accessKeyStore.remove(accessKeyId);

        return new DoHandshakeResponseDTO(
                CryptoUtils.encodeBase64(secureEnvelope.payload()),
                CryptoUtils.encodeBase64(secureEnvelope.nonce())
        );
    }
}
