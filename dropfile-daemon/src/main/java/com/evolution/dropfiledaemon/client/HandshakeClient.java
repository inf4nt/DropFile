package com.evolution.dropfiledaemon.client;

import com.evolution.dropfile.common.dto.DoHandshakeRequestDTO;
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
    public HttpResponse<byte[]> getIdentity(URI address) {
        URI addressURI = address.resolve("/handshake");

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(addressURI)
                .GET()
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshake(URI addressURI,
                                          DoHandshakeRequestDTO doHandshakeRequestDTO) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(addressURI.resolve("/handshake"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(doHandshakeRequestDTO))
                )
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }
}
