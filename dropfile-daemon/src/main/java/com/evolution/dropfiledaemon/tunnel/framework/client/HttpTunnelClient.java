package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.io.InputStreamPipeline;
import com.evolution.dropfile.common.io.WatchdogInputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.server.ServerTunnelRestController;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
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
    public InputStream stream(Request request) throws IOException {
        Objects.requireNonNull(request, "Request must not be null");

        HttpTunnelRequestContext httpTunnelRequestContext = buildHttpTunnelRequestContext(request);

        HttpResponse<InputStream> httpResponse = null;
        try {
            try {
                httpResponse = httpClient.send(httpTunnelRequestContext.request(), HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Call interrupted", e);
            } catch (HttpConnectTimeoutException e) {
                HttpRequest httpRequest = httpTunnelRequestContext.request();
                long timeout = httpRequest.timeout().map(Duration::toMillis).orElse(0L);
                String message = "HTTP connect timed out during call %s %s timeout %s millis"
                        .formatted(httpRequest.method(), httpRequest.uri(), timeout);
                throw new HttpConnectTimeoutException(message).initCause(e);
            } catch (ConnectException e) {
                throw new ConnectException("Target address is unreachable").initCause(e);
            }

            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException("Unexpected HTTP response status code. Expected: 200 actual: %s".formatted(
                        httpResponse.statusCode()
                ));
            }

            InputStream inputStreamResponse = getInputStreamResponse(httpResponse.body(),
                    httpTunnelRequestContext.fingerprint(),
                    httpTunnelRequestContext.secretKey()
            );
            validateInputStream(httpTunnelRequestContext.requestId(), inputStreamResponse);
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
            HttpRequest httpRequest = httpTunnelRequestContext.request();
            String message = "Tunnel call failed. Fingerprint %s command %s'. Reason '%s'. Method %s uri %s timeout %s".formatted(
                    request.getFingerprint(),
                    request.getCommand(),
                    throwable.getMessage(),
                    httpRequest.method(),
                    httpRequest.uri(),
                    httpRequest.timeout().map(it -> it.toMillis()).orElse(0L)
            );

            if (throwable instanceof IOException ioException) {
                throw new IOException(message, ioException);
            }

            if (throwable instanceof Error error) {
                throw error;
            }

            throw new RuntimeException(message, throwable);
        }
    }

    private InputStream getInputStreamResponse(InputStream inputStream,
                                               String fingerprint,
                                               SecretKey secretKey) {
        return InputStreamPipeline
                .from(inputStream)
                .add(in -> new WatchdogInputStream(
                        in,
                        daemonApplicationProperties.daemonTunnelClientStreamMaxSize,
                        Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientStreamDeadlineTimeoutMillis)
                ))
                .add(in -> tunnelTrafficMonitor.inputStreamWrapper(fingerprint, in))
                .add(in -> {
                    byte[] decrypt = cryptoTunnel.decrypt(in, secretKey);
                    return new ByteArrayInputStream(decrypt);
                })
                .add(in -> {
                    if (daemonApplicationProperties.daemonTunnelClientCompressEnabled) {
                        return compressTunnelService.decompress(in);
                    }
                    return in;
                })
                .add(in -> new WatchdogInputStream(
                        in,
                        daemonApplicationProperties.daemonTunnelClientStreamMaxSize
                ))
                .get();
    }

    private SecureEnvelope encrypt(UUID requestId, Request request, SecretKey secretKey) throws IOException {
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

    private void validateInputStream(UUID requestId, InputStream inputStream) throws IOException {
        byte[] expectedRequestIdBytes = requestId.toString().getBytes(StandardCharsets.UTF_8);

        byte[] actualRequestIdBytes = inputStream.readNBytes(expectedRequestIdBytes.length);
        if (actualRequestIdBytes.length < expectedRequestIdBytes.length
                || !MessageDigest.isEqual(actualRequestIdBytes, expectedRequestIdBytes)) {
            throw new SecurityException("Tunnel response request ID mismatch or stream truncated");
        }
    }

    private HttpTunnelRequestContext buildHttpTunnelRequestContext(Request request) {
        try {
            HandshakeTrustedOutStore.TrustedOut trustedOut = getTrustedOut(request.getFingerprint());
            SecretKey secretKey = getSecretKey(trustedOut);

            UUID requestId = UUID.randomUUID();

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

            return new HttpTunnelRequestContext(request.getFingerprint(), requestId, httpRequest, secretKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tunnel request. Fingerprint %s command %s".formatted(request.getFingerprint(), request.getCommand()));
        }
    }

    private record HttpTunnelRequestContext(String fingerprint,
                                            UUID requestId,
                                            HttpRequest request,
                                            SecretKey secretKey) {

    }
}
