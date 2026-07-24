package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.evolution.dropfiledaemon.tunnel.framework.server.command.CommandHandlerExecutor;
import com.evolution.dropfiledaemon.tunnel.framework.server.compress.CompressTunnelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher, Purgeable {

    private final Map<String, Instant> requests = new ConcurrentHashMap<>();

    // TODO create an env var
    // 1 hour + 15 min grace period
    private static final Duration SESSION_TTL = Duration.ofHours(1).plusMinutes(15);

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final CommandHandlerExecutor commandHandlerExecutor;

    private final CryptoTunnel cryptoTunnel;

    private final CompressTunnelService compressTunnelService;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ObjectMapper objectMapper;

    @Override
    public void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException {
        String fingerprint = requestDTO.fingerprint();

        Map.Entry<String, HandshakeTrustedInStore.TrustedIn> trustedInEntry = handshakeTrustedInStore
                .getRequired(fingerprint);

        validateSession(trustedInEntry);

        SecretKey secretKey = getSecretKey(trustedInEntry.getValue());

        TunnelRequestDTO.Payload tunnelRequestPayload = decrypt(requestDTO, secretKey);
        validatePayloadTimestamp(tunnelRequestPayload.timestamp());
        validateRequestForReplyAttack(fingerprint, tunnelRequestPayload.requestId());

        commandHandler(fingerprint, tunnelRequestPayload, secretKey, outputStream);
    }

    private void commandHandler(String fingerprint,
                                TunnelRequestDTO.Payload tunnelRequestPayload,
                                SecretKey secretKey,
                                OutputStream outputStream) throws IOException {
        Object handlerResult = commandHandlerExecutor.handle(tunnelRequestPayload);

        try (InputStream inputStreamResult = handlerResultToInputStream(handlerResult);
             OutputStream monitorStream = tunnelTrafficMonitor.outputStreamWrapper(fingerprint, outputStream);
             OutputStream encryptStream = cryptoTunnel.encryptWrapper(monitorStream, secretKey)) {

            if (tunnelRequestPayload.configuration().compress()) {
                try (OutputStream compressStream = compressTunnelService.compressWrapper(encryptStream)) {
                    inputStreamResult.transferTo(compressStream);
                    compressStream.flush();
                }
            } else {
                inputStreamResult.transferTo(encryptStream);
                encryptStream.flush();
            }
        }
    }

    private void validateSession(Map.Entry<String, HandshakeTrustedInStore.TrustedIn> trustedInEntry) {
        String fingerprint = trustedInEntry.getKey();
        HandshakeTrustedInStore.TrustedIn trustedIn = trustedInEntry.getValue();
        if (Instant.now().isAfter(trustedIn.sessionUpdated().plus(SESSION_TTL))) {
            throw new RuntimeException("Session has expired for fingerprint: " + fingerprint);
        }
    }

    private SecretKey getSecretKey(HandshakeTrustedInStore.TrustedIn trustedIn) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(trustedIn.session().privateDH()),
                CryptoECDH.getPublicKey(trustedIn.session().remotePublicDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    @SneakyThrows
    private TunnelRequestDTO.Payload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.Payload.class);
    }

    private void validatePayloadTimestamp(long timestamp) {
        long requestTime = Math.abs(System.currentTimeMillis() - timestamp);
        int maxLifetime = daemonApplicationProperties.daemonTunnelServerPayloadLifeTime;
        if (requestTime > maxLifetime) {
            throw new RuntimeException(
                    String.format("Tunnel request timeout. Expected max %sms, actual drift %sms", maxLifetime, requestTime)
            );
        }
    }

    @SneakyThrows
    private InputStream handlerResultToInputStream(Object handlerResult) {
        if (handlerResult instanceof InputStream inputStream) {
            return inputStream;
        }
        if (handlerResult instanceof byte[] arrayResult) {
            return new ByteArrayInputStream(arrayResult);
        }
        if (handlerResult instanceof String stringResult) {
            return new ByteArrayInputStream(stringResult.getBytes(StandardCharsets.UTF_8));
        }

        byte[] bytes = objectMapper.writeValueAsBytes(handlerResult);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void purge() {
        long maxLifetime = daemonApplicationProperties.daemonTunnelServerPayloadLifeTime;
        long cutoff = System.currentTimeMillis() - maxLifetime;

        requests.values().removeIf(instant -> instant.toEpochMilli() < cutoff);
    }

    private void validateRequestForReplyAttack(String fingerprint, String requestId) {
        String requestKey = getRequestKey(fingerprint, requestId);

        Instant existing = requests.putIfAbsent(requestKey, Instant.now());

        if (existing != null) {
            log.warn("Replay attack detected for key: {}", requestKey);
            throw new RuntimeException("Replay attack detected!");
        }
    }

    private String getRequestKey(String fingerprint, String requestId) {
        return fingerprint + ":" + requestId;
    }
}
