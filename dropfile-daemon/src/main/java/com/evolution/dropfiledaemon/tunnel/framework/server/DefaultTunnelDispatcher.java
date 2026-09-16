package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnelV2;
import com.evolution.dropfile.common.io.CloseShieldOutputStream;
import com.evolution.dropfile.common.io.InterruptibleOutputStream;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.service.ReplyAttackGuard;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelDispatcherContext;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.evolution.dropfiledaemon.tunnel.framework.server.command.CommandHandlerExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    // 1 hour + 15 min grace period
    private static final Duration SESSION_TTL = Duration.ofHours(1).plusMinutes(15);

    private final CommandHandlerExecutor commandHandlerExecutor;

    private final CryptoTunnelV2 cryptoTunnel;

    private final CompressTunnelService compressTunnelService;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final HandshakeSessionInStore handshakeSessionInStore;

    private final ReplyAttackGuard replyAttackGuard;

    private final ObjectMapper objectMapper;

    @Override
    public TunnelDispatcherContext dispatch(TunnelRequestDTO requestDTO) {
        InputStream inputStream = null;
        String command = null;
        String fingerprint = null;
        try {
            Map.Entry<String, HandshakeTrustedInStore.TrustedIn> trustedInEntry = handshakeTrustedInStore
                    .getRequired(requestDTO.fingerprint());

            fingerprint = trustedInEntry.getKey();

            validateSession(trustedInEntry);

            TunnelSessionKeys sessionKeys = getSessionKeys(fingerprint, trustedInEntry.getValue());
            byte[] aadCurrentFingerprint = CommonUtils.getFingerprint(trustedInEntry.getValue().handshake().publicRSA())
                    .getBytes(StandardCharsets.UTF_8);

            TunnelRequestDTO.Payload tunnelRequestPayload = decrypt(requestDTO, aadCurrentFingerprint, sessionKeys.clientKey());

            command = tunnelRequestPayload.command();
            replyAttackGuard.tunnelDispatcherRequest(fingerprint, tunnelRequestPayload);

            Object handlerResult = commandHandlerExecutor.handle(tunnelRequestPayload);

            inputStream = handlerResultToInputStream(handlerResult);

            return new TunnelDispatcherContext(
                    fingerprint,
                    sessionKeys.serverKey(),
                    tunnelRequestPayload,
                    inputStream
            );
        } catch (Throwable throwable) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable closeThrowable) {
                    log.error("Failed to close inputstream body during failure cleanup. Fingerprint {} command {}",
                            fingerprint,
                            Objects.requireNonNullElse(command, "None"),
                            closeThrowable
                    );
                    throwable.addSuppressed(closeThrowable);
                }
            }
            String message = "Failed to process tunnel request. Fingerprint %s command %s".formatted(
                    Objects.requireNonNullElse(fingerprint, "None"),
                    Objects.requireNonNullElse(command, "None")
            );
            throw CommonUtils.toRuntimeException(message, throwable);
        }
    }

    @Override
    public void transfer(TunnelDispatcherContext context, OutputStream outputStreamArgument) throws IOException {
        String fingerprint = context.fingerprint();
        SecretKey serverSecretKey = context.secretKey();
        TunnelRequestDTO.Payload tunnelRequestPayload = context.requestPayload();

        byte[] aadRemoteFingerprint = fingerprint.getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = context.inputStream()) {
            InterruptibleOutputStream interruptibleOutputStream = InterruptibleOutputStream.stream(
                    CloseShieldOutputStream.stream(outputStreamArgument)
            );

            try (OutputStream monitorStream = tunnelTrafficMonitor.outputStreamWrapper(fingerprint, interruptibleOutputStream);
                 OutputStream encryptStream = cryptoTunnel.encryptWrapper(
                         CloseShieldOutputStream.stream(monitorStream),
                         aadRemoteFingerprint,
                         serverSecretKey
                 );
                 OutputStream compressOutputStream = compress(tunnelRequestPayload.configuration(), CloseShieldOutputStream.stream(encryptStream))) {

                writeMarkersToOutputStream(tunnelRequestPayload.requestId(), compressOutputStream);
                inputStream.transferTo(compressOutputStream);
                compressOutputStream.flush();
            }
        } catch (Throwable throwable) {
            String message = "Failed to transfer data to tunnel outputstream. Fingerprint %s command %s".formatted(
                    fingerprint,
                    tunnelRequestPayload.command()
            );
            if (throwable instanceof IOException ioException) {
                throw new IOException(message, ioException);
            }
            throw CommonUtils.toRuntimeException(message, throwable);
        }
    }

    private void validateSession(Map.Entry<String, HandshakeTrustedInStore.TrustedIn> trustedInEntry) {
        String fingerprint = trustedInEntry.getKey();
        HandshakeTrustedInStore.TrustedIn trustedIn = trustedInEntry.getValue();
        if (Instant.now().isAfter(trustedIn.sessionUpdated().plus(SESSION_TTL))) {
            throw new SecurityException("Session has expired for fingerprint: " + fingerprint);
        }
    }

    private TunnelSessionKeys getSessionKeys(String fingerprint, HandshakeTrustedInStore.TrustedIn trustedIn) {
        HandshakeSessionInStore.SessionIn sessionIn = handshakeSessionInStore.get(fingerprint)
                .map(Map.Entry::getValue)
                .filter(it -> it.handshakeId().equals(trustedIn.handshakeId()))
                .orElseThrow(() -> new NoSuchElementException("No session found " + fingerprint));

        SecretKey clientKey = cryptoTunnel.secretKey(sessionIn.clientKey());
        SecretKey serverKey = cryptoTunnel.secretKey(sessionIn.serverKey());

        return new TunnelSessionKeys(clientKey, serverKey);
    }

    @SneakyThrows
    private TunnelRequestDTO.Payload decrypt(TunnelRequestDTO requestDTO, byte[] aad, SecretKey clientSecretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                aad,
                clientSecretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.Payload.class);
    }

    @SneakyThrows
    private InputStream handlerResultToInputStream(@Nullable Object handlerResult) {
        if (handlerResult == null) {
            return InputStream.nullInputStream();
        }

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

    private void writeMarkersToOutputStream(UUID requestId, OutputStream outputStream) throws IOException {
        outputStream.write(requestId.toString().getBytes(StandardCharsets.UTF_8));
    }

    private record TunnelSessionKeys(SecretKey clientKey, SecretKey serverKey) {
    }
}