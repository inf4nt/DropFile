package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.InterruptibleOutputStream;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.service.ReplyAttackGuard;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcherContext;
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

@Slf4j
@RequiredArgsConstructor
@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    // TODO create an env var
    // 1 hour + 15 min grace period
    private static final Duration SESSION_TTL = Duration.ofHours(1).plusMinutes(15);

    private final CommandHandlerExecutor commandHandlerExecutor;

    private final CryptoTunnel cryptoTunnel;

    private final CompressTunnelService compressTunnelService;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ReplyAttackGuard replyAttackGuard;

    private final ObjectMapper objectMapper;

    @Override
    public TunnelDispatcherContext dispatch(TunnelRequestDTO requestDTO) {
        InputStream inputStream = null;
        try {
            String fingerprint = requestDTO.fingerprint();

            Map.Entry<String, HandshakeTrustedInStore.TrustedIn> trustedInEntry = handshakeTrustedInStore
                    .getRequired(fingerprint);

            validateSession(trustedInEntry);

            SecretKey secretKey = getSecretKey(trustedInEntry.getValue());

            TunnelRequestDTO.Payload tunnelRequestPayload = decrypt(requestDTO, secretKey);
            replyAttackGuard.tryToAddTunnelDispatcherRequest(fingerprint, tunnelRequestPayload);

            Object handlerResult = commandHandlerExecutor.handle(tunnelRequestPayload);

            inputStream = handlerResultToInputStream(handlerResult);
            return new TunnelDispatcherContext(
                    fingerprint,
                    secretKey,
                    tunnelRequestPayload,
                    inputStream
            );
        } catch (Throwable throwable) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throwable.addSuppressed(e);
                }
            }
            throw CommonUtils.toRuntimeException(throwable);
        }
    }

    @Override
    public void transfer(TunnelDispatcherContext context, OutputStream outputStreamArgument) {
        try {
            InterruptibleOutputStream outputStream = InterruptibleOutputStream.stream(
                    CloseShieldOutputStream.stream(outputStreamArgument)
            );

            String fingerprint = context.getFingerprint();
            SecretKey secretKey = context.getSecretKey();
            TunnelRequestDTO.Payload tunnelRequestPayload = context.getRequestPayload();

            InputStream inputStream = context.getInputStream();

            OutputStream monitorStream = tunnelTrafficMonitor.outputStreamWrapper(fingerprint, outputStream);
            OutputStream encryptStream = cryptoTunnel.encryptWrapper(CloseShieldOutputStream.stream(monitorStream), secretKey);
            OutputStream compressOutputStream = compress(tunnelRequestPayload.configuration(), CloseShieldOutputStream.stream(encryptStream));

            writeMarkersToOutputStream(tunnelRequestPayload.requestId(), compressOutputStream);

            inputStream.transferTo(compressOutputStream);

            compressOutputStream.flush();
            compressOutputStream.close();
            encryptStream.close();
            monitorStream.close();
        } catch (Throwable throwable) {
            throw CommonUtils.toRuntimeException(throwable);
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
        byte[] secret = trustedIn.session().sessionKey();
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

    private OutputStream compress(TunnelRequestDTO.Configuration configuration, OutputStream outputStream) throws IOException {
        if (configuration.compress()) {
            return compressTunnelService.compressWrapper(outputStream);
        }

        return CloseShieldOutputStream.stream(outputStream);
    }

    private void writeMarkersToOutputStream(String requestId, OutputStream outputStream) throws IOException {
        outputStream.write(requestId.getBytes(StandardCharsets.UTF_8));
    }
}
