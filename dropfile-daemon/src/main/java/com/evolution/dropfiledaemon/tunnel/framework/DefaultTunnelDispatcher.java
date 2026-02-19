package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    private static final int MAX_PAYLOAD_LIFETIME = 30_000;

    private final ApplicationConfigStore applicationConfigStore;

    private final CommandHandlerExecutor commandHandlerExecutor;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @SneakyThrows
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

        try (InputStream inputStreamResult = handlerResultToInputStream(handlerResult)) {
            cryptoTunnel.encrypt(inputStreamResult, outputStream, secretKey);
        }
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
        HandshakeSessionStore.SessionValue session = applicationConfigStore.getHandshakeStore().sessionInStore()
                .getRequired(fingerprint)
                .getValue();
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(session.privateDH()),
                CryptoECDH.getPublicKey(session.remotePublicDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    @SneakyThrows
    private TunnelRequestDTO.TunnelRequestPayload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.TunnelRequestPayload.class);
    }
}
