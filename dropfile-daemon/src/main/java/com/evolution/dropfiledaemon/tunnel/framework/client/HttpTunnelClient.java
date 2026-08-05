package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.io.WatchdogInputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.ServerTunnelRestController;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.evolution.dropfiledaemon.tunnel.framework.server.compress.CompressTunnelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpTunnelClient implements TunnelClient {

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final CryptoTunnel cryptoTunnel;

    private final HttpClient httpClient;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final CompressTunnelService compressTunnelService;

    private final ObjectMapper objectMapper;

    @Override
    public InputStream stream(Request request) {
        Objects.requireNonNull(request, "Request must not be null");

        HttpResponse<InputStream> httpResponse = null;
        try {
            String fingerprint = request.getFingerprint();

            HandshakeTrustedOutStore.TrustedOut trustedOut = getTrustedOut(fingerprint);
            SecretKey secretKey = getSecretKey(trustedOut);

            String requestId = UUID.randomUUID().toString();

            SecureEnvelope secureEnvelope = encrypt(requestId, request, secretKey);

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CommonUtils.getFingerprint(trustedOut.handshake().publicRSA()),
                    secureEnvelope.payload(),
                    secureEnvelope.nonce()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            CommonUtils.joinPaths(
                                    trustedOut.addressURI().toString(),
                                    ServerTunnelRestController.TUNNEL_ENDPOINT
                            )
                    ))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(tunnelRequestDTO))
                    )
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientHttpRequestTimeoutMillis))
                    .build();

            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException(
                        "Unexpected tunnel HTTP response status code. Expected: 200, actual: " + httpResponse.statusCode()
                );
            }

            InputStream inputStreamResponse = getInputStreamResponse(httpResponse.body(), fingerprint, secretKey);
            validateInputStream(requestId, inputStreamResponse);
            return inputStreamResponse;
        } catch (Throwable throwable) {
            if (httpResponse != null) {
                try {
                    httpResponse.body().close();
                } catch (Throwable closeThrowable) {
                    log.error(
                            "Failed to close HTTP response body stream during failure cleanup. Fingerprint {} command {}",
                            request.getFingerprint(),
                            request.getCommand(),
                            closeThrowable
                    );
                    throwable.addSuppressed(closeThrowable);
                }
            }
            String message = "Tunnel streaming request failed. Fingerprint %s command %s".formatted(
                    request.getFingerprint(),
                    request.getCommand()
            );
            throw CommonUtils.toRuntimeException(message, throwable);
        }
    }

    private InputStream getInputStreamResponse(InputStream inputStream,
                                               String fingerprint,
                                               SecretKey secretKey) throws IOException {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                inputStream,
                daemonApplicationProperties.daemonTunnelClientStreamMaxSize,
                Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientStreamDeadlineTimeoutMillis)
        );
        InputStream trafficMonitorInputStream = tunnelTrafficMonitor.inputStreamWrapper(fingerprint, watchdogInputStream);
        InputStream decryptedStream = cryptoTunnel.decrypt(trafficMonitorInputStream, secretKey);

        if (daemonApplicationProperties.daemonTunnelClientCompressEnabled) {
            return compressTunnelService.decompress(decryptedStream);
        }
        return decryptedStream;
    }

    private SecureEnvelope encrypt(String requestId, Request request, SecretKey secretKey) throws JsonProcessingException {
        byte[] payload = switch (request.getBody()) {
            case null -> null;
            case String string -> string.getBytes(StandardCharsets.UTF_8);
            case byte[] byteArray -> byteArray;
            default -> objectMapper.writeValueAsBytes(request.getBody());
        };

        return cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(
                        new TunnelRequestDTO.Payload(
                                requestId,
                                request.getCommand(),
                                payload,
                                new TunnelRequestDTO.Configuration(
                                        daemonApplicationProperties.daemonTunnelClientCompressEnabled
                                ),
                                System.currentTimeMillis()
                        )
                ),
                secretKey
        );
    }

    private SecretKey getSecretKey(HandshakeTrustedOutStore.TrustedOut trustedOut) {
        byte[] secret = trustedOut.session().sessionKey();
        return cryptoTunnel.secretKey(secret);
    }

    private HandshakeTrustedOutStore.TrustedOut getTrustedOut(String fingerprint) {
        return handshakeTrustedOutStore.getRequired(fingerprint).getValue();
    }

    @SneakyThrows
    private void validateInputStream(String requestId, InputStream inputStream) {
        byte[] expectedRequestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);

        byte[] actualRequestIdBytes = inputStream.readNBytes(expectedRequestIdBytes.length);
        if (actualRequestIdBytes.length < expectedRequestIdBytes.length
                || !MessageDigest.isEqual(actualRequestIdBytes, expectedRequestIdBytes)) {
            throw new SecurityException("Tunnel response request ID mismatch or stream truncated");
        }
    }
}
