package com.evolution.dropfiledaemon.handshake.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.ServerHandshakeRestController;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class HandshakeClient {

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
                .timeout(Duration.ofMillis(daemonApplicationProperties.daemonHandshakeClientHttpRequestTimeoutMillis))
                .build();

        HttpResponse<byte[]> httpResponse = execute(httpRequest);
        return objectMapper.readValue(httpResponse.body(), responseClass);
    }

    @SneakyThrows
    private HttpResponse<byte[]> execute(HttpRequest httpRequest) {
        HttpResponse<byte[]> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (ConnectException connectException) {
            throw new IOException("Unable to process handshake. The address is unreachable %s %s"
                    .formatted(httpRequest.method(), httpRequest.uri()), connectException);
        }

        if (httpResponse.statusCode() != 200) {
            throw new IllegalStateException(String.format("Handshake %s %s failed: status code %s",
                    httpRequest.method(), httpRequest.uri(), httpResponse.statusCode()));
        }
        byte[] body = httpResponse.body();
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("Handshake server returned 200 OK but empty body %s %s".formatted(httpRequest.method(), httpRequest.uri()));
        }
        return httpResponse;
    }
}