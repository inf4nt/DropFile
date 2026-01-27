package com.evolution.dropfiledaemon.handshake.client;

import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class HandshakeClient {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public HandshakeClient(HttpClient httpClient,
                           ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshake(URI addressURI,
                                          HandshakeRequestDTO handshakeRequestDTO) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(addressURI.resolve("/handshake"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(handshakeRequestDTO))
                )
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }
}
