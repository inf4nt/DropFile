package com.evolution.dropfiledaemon.handshake.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.io.WatchdogInputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.server.ServerHandshakeRestController;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class HandshakeClient {

    // TODO add env vars
    private static final int HANDSHAKE_WATCHDOG_RESPONSE_LIMIT = 8_192;

    private static final Duration HANDSHAKE_WATCHDOG_RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public HandshakeResponseDTO handshake(URI addressURI,
                                          HandshakeRequestDTO handshakeRequestDTO) {
        return post(
                URI.create(CommonUtils.joinPaths(
                        addressURI.toString(),
                        ServerHandshakeRestController.HANDSHAKE_ENDPOINT
                )),
                handshakeRequestDTO,
                HandshakeResponseDTO.class
        );
    }

    public HandshakeSessionDTO.Session handshakeSession(URI addressURI,
                                                        HandshakeSessionDTO.Session session) {
        return post(
                URI.create(CommonUtils.joinPaths(
                        addressURI.toString(),
                        ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT
                )),
                session,
                HandshakeSessionDTO.Session.class
        );
    }

    @SneakyThrows
    private <T> T post(URI uri, Object requestBody, Class<T> responseClass) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(requestBody)))
                .header("Content-Type", "application/json")
                .timeout(daemonApplicationProperties.daemonHandshakeClientHttpRequestTimeout)
                .build();

        byte[] payload = execute(httpRequest);
        return objectMapper.readValue(payload, responseClass);
    }

    private byte[] execute(HttpRequest httpRequest) throws IOException {
        try (InputStream inputStream = doExecute(httpRequest)) {
            byte[] payload = inputStream.readAllBytes();
            if (payload.length == 0) {
                throw new IllegalStateException("Handshake server returned 200 OK but empty body %s %s"
                        .formatted(httpRequest.method(), httpRequest.uri()));
            }
            return payload;
        }
    }

    @SneakyThrows
    private InputStream doExecute(HttpRequest httpRequest) {
        HttpResponse<InputStream> httpResponse = null;
        try {
            try {
                httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            } catch (ConnectException e) {
                throw new ConnectException("Handshake client failed. Target address is unreachable %s %s"
                        .formatted(httpRequest.method(), httpRequest.uri()));
            } catch (HttpConnectTimeoutException e) {
                throw new HttpConnectTimeoutException("HTTP connect timed out during handshake client call %s %s timeout %s millis"
                        .formatted(httpRequest.method(), httpRequest.uri(), httpRequest.timeout().orElseThrow().toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Handshake client interrupted: %s %s"
                        .formatted(httpRequest.method(), httpRequest.uri()), e);
            } catch (IOException e) {
                throw new IOException("I/O error during handshake %s %s"
                        .formatted(httpRequest.method(), httpRequest.uri()), e);
            }

            int statusCode = httpResponse.statusCode();
            if (statusCode != 200) {
                throw new IllegalStateException("Handshake %s %s failed with status code %s. Expected 200"
                        .formatted(httpRequest.method(), httpRequest.uri(), statusCode));
            }
            return new WatchdogInputStream(
                    httpResponse.body(),
                    HANDSHAKE_WATCHDOG_RESPONSE_LIMIT,
                    HANDSHAKE_WATCHDOG_RESPONSE_TIMEOUT
            );
        } catch (Throwable throwable) {
            if (httpResponse != null) {
                try {
                    httpResponse.body().close();
                } catch (Throwable closeThrowable) {
                    throwable.addSuppressed(closeThrowable);
                }
            }
            throw throwable;
        }
    }
}