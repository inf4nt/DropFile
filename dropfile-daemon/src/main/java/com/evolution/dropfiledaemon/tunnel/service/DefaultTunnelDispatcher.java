package com.evolution.dropfiledaemon.tunnel.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    private final ActionHandlerExecutor actionHandlerExecutor;

    private final CryptoTunnel cryptoTunnel;

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final ObjectMapper objectMapper;

    public DefaultTunnelDispatcher(ActionHandlerExecutor actionHandlerExecutor,
                                   CryptoTunnel cryptoTunnel,
                                   HandshakeStore handshakeStore,
                                   KeysConfigStore keysConfigStore,
                                   ObjectMapper objectMapper) {
        this.actionHandlerExecutor = actionHandlerExecutor;
        this.cryptoTunnel = cryptoTunnel;
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

        Object body = actionHandlerExecutor.handle(payload);
        Objects.requireNonNull(body);

        return encrypt(body, secretKey);
    }

    private SecretKey getSecretKey(String fingerprint) {
        Map.Entry<String, TrustedInKeyValueStore.TrustedInValue> trustedInValue = handshakeStore
                .trustedInStore()
                .get(fingerprint)
                .orElseThrow(() -> new RuntimeException(
                        "No trusted-in connection found for fingerprint: " + fingerprint
                ));
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedInValue.getValue().publicKeyDH())
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
                CommonUtils.encodeBase64(encrypt.payload()),
                CommonUtils.encodeBase64(encrypt.nonce())
        );
    }

    @SneakyThrows
    private TunnelRequestDTO.TunnelRequestPayload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                CommonUtils.decodeBase64(requestDTO.requestPayload()),
                CommonUtils.decodeBase64(requestDTO.nonce()),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.TunnelRequestPayload.class);
    }
}
