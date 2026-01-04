package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelResponseDTO;
import com.evolution.dropfiledaemon.tunnel.framework.handler.GlobalActionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    private final CryptoTunnel cryptoTunnel;

    private final GlobalActionHandler globalActionHandler;

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultTunnelDispatcher(CryptoTunnel cryptoTunnel,
                                   GlobalActionHandler globalActionHandler,
                                   HandshakeStore handshakeStore,
                                   KeysConfigStore keysConfigStore,
                                   ObjectMapper objectMapper) {
        this.cryptoTunnel = cryptoTunnel;
        this.globalActionHandler = globalActionHandler;
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

        Object body = globalActionHandler.handle(payload);

        return encrypt(body, secretKey);
    }

    private SecretKey getSecretKey(String fingerprint) {
        TrustedInKeyValueStore.TrustedInValue trustedInValue = handshakeStore
                .trustedInStore()
                .get(fingerprint)
                .orElse(null);
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
        if (object == null) {
            payload = new byte[0];
        } else if (object instanceof byte[]) {
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
