package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    private static final int MAX_PAYLOAD_LIFETIME = 30_000;

    private final CommandHandlerExecutor commandHandlerExecutor;

    private final CryptoTunnel cryptoTunnel;

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final ObjectMapper objectMapper;

    public DefaultTunnelDispatcher(CommandHandlerExecutor commandHandlerExecutor,
                                   CryptoTunnel cryptoTunnel,
                                   HandshakeStore handshakeStore,
                                   KeysConfigStore keysConfigStore,
                                   ObjectMapper objectMapper) {
        this.commandHandlerExecutor = commandHandlerExecutor;
        this.cryptoTunnel = cryptoTunnel;
        this.handshakeStore = handshakeStore;
        this.keysConfigStore = keysConfigStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) {
        SecretKey secretKey = getSecretKey(requestDTO.fingerprint());

        TunnelRequestDTO.TunnelRequestPayload payload = decrypt(requestDTO, secretKey);

        long requestTime = Math.abs(System.currentTimeMillis() - payload.timestamp());
        if (requestTime > MAX_PAYLOAD_LIFETIME) {
            throw new RuntimeException(
                    String.format(
                            "Tunnel request timeout exception. Expected %s actual %s",
                            MAX_PAYLOAD_LIFETIME, requestTime
                    )
            );
        }

        Object handlerResult = commandHandlerExecutor.handle(payload);

        InputStream inputStreamResult = handlerResultToInputStream(handlerResult);

        cryptoTunnel.encrypt(inputStreamResult, outputStream, secretKey);
    }

    @SneakyThrows
    private InputStream handlerResultToInputStream(Object handlerResult) {
        if (handlerResult instanceof InputStream) {
            return (InputStream) handlerResult;
        }
        if (handlerResult instanceof byte[] arrayResult) {
            return new ByteArrayInputStream(arrayResult);
        }
        if (handlerResult instanceof String stringResult) {
            return new ByteArrayInputStream(stringResult.getBytes());
        }

        byte[] bytes = objectMapper.writeValueAsBytes(handlerResult);
        return new ByteArrayInputStream(bytes);
    }

    private SecretKey getSecretKey(String fingerprint) {
        Map.Entry<String, TrustedInKeyValueStore.TrustedInValue> trustedInValue = handshakeStore
                .trustedInStore()
                .getRequired(fingerprint);
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedInValue.getValue().publicKeyDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    @SneakyThrows
    private TunnelRequestDTO.TunnelRequestPayload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                requestDTO.requestPayload(),
                requestDTO.nonce(),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.TunnelRequestPayload.class);
    }
}
