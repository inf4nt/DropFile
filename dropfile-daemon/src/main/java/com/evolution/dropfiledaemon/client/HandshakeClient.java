package com.evolution.dropfiledaemon.client;

import com.evolution.dropfile.configuration.dto.HandshakeRequestApprovedDTO;
import com.evolution.dropfile.configuration.dto.HandshakeRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;

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
    public HttpResponse<Void> handshakeRequest(URI handshakeNodeAddressURI,
                                               int currentNodePort,
                                               PublicKey currentNodePublicKey) {
        URI handshakeURI = handshakeNodeAddressURI.resolve("/handshake/request");

        byte[] payload = objectMapper.writeValueAsBytes(
                new HandshakeRequestDTO(
                        currentNodePublicKey.getEncoded(),
                        currentNodePort
                )
        );

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<Void> handshakeRequestApproved(URI nodeAddressURI,
                                                       PublicKey publicKey,
                                                       byte[] encryptMessage) {
        byte[] payload = objectMapper.writeValueAsBytes(new HandshakeRequestApprovedDTO(
                publicKey.getEncoded(),
                encryptMessage
        ));

        URI handshakeRequestApprovedURI = nodeAddressURI.resolve("/handshake/request/approved");

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeRequestApprovedURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }
}
