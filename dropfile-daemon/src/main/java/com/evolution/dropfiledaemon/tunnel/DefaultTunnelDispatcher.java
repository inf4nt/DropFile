package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.handler.TunnelActionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    private final CryptoTunnel cryptoTunnel;

    private final TunnelActionHandler tunnelActionHandler;

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultTunnelDispatcher(CryptoTunnel cryptoTunnel,
                                   TunnelActionHandler tunnelActionHandler,
                                   HandshakeStore handshakeStore,
                                   KeysConfigStore keysConfigStore,
                                   ObjectMapper objectMapper) {
        this.cryptoTunnel = cryptoTunnel;
        this.tunnelActionHandler = tunnelActionHandler;
        this.handshakeStore = handshakeStore;
        this.keysConfigStore = keysConfigStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public TunnelResponseDTO dispatch(TunnelRequestDTO requestDTO) {
        SecretKey secretKey = getSecretKey(requestDTO.fingerprint());

        TunnelRequestDTO.TunnelRequestPayload payload = decrypt(requestDTO, secretKey);

        if (Math.abs(System.currentTimeMillis() - payload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        Object body = tunnelActionHandler.handle(payload);

        Objects.requireNonNull(body);

        return encrypt(body, secretKey);
    }

    private SecretKey getSecretKey(String fingerprint) {
        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore
                .trustedInStore()
                .get(fingerprint).orElse(null);
        if (trustedInValue == null) {
            throw new RuntimeException("No trusted in key store found for fingerprint " + fingerprint);
        }
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedInValue.publicKeyDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    @SneakyThrows
    private TunnelResponseDTO encrypt(Object object, SecretKey secretKey) {
        byte[] payload;
        if (object instanceof byte[]) {
            payload = (byte[]) object;
        } else if (object instanceof String) {
            payload = object.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            payload = objectMapper.writeValueAsBytes(object);
        }
        SecureEnvelope encrypt = cryptoTunnel.encrypt(payload, secretKey);
        return new TunnelResponseDTO(
                CryptoUtils.encodeBase64(encrypt.payload()),
                CryptoUtils.encodeBase64(encrypt.nonce())
        );
    }

    @SneakyThrows
    private TunnelRequestDTO.TunnelRequestPayload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                CryptoUtils.decodeBase64(requestDTO.requestPayload()),
                CryptoUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.TunnelRequestPayload.class);
    }
}
