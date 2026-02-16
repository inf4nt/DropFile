package com.evolution.dropfiledaemon.handshake.client;

import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class HandshakeClient {

    private static final Duration HANDSHAKE_HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public HandshakeResponseDTO handshake(URI addressURI,
                                          HandshakeRequestDTO handshakeRequestDTO) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(addressURI.resolve("/handshake"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(handshakeRequestDTO))
                )
                .header("Content-Type", "application/json")
                .timeout(HANDSHAKE_HTTP_REQUEST_TIMEOUT)
                .build();

        HttpResponse<byte[]> httpResponse = execute(httpRequest);
        return objectMapper.readValue(httpResponse.body(), HandshakeResponseDTO.class);
    }

    @SneakyThrows
    public HandshakeSessionDTO.Session handshakeSession(URI addressURI,
                                                        HandshakeSessionDTO.Session session) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(addressURI.resolve("/handshake/session"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(session))
                )
                .timeout(HANDSHAKE_HTTP_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<byte[]> httpResponse = execute(httpRequest);
        return objectMapper.readValue(httpResponse.body(), HandshakeSessionDTO.Session.class);
    }

    @SneakyThrows
    private HttpResponse<byte[]> execute(HttpRequest httpRequest) {
        HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (httpResponse.statusCode() != 200) {
            throw new RuntimeException(String.format("Handshake %s %s failed: status code %s", httpRequest.method(), httpRequest.uri(), httpResponse.statusCode()));
        }
        return httpResponse;
    }
}
