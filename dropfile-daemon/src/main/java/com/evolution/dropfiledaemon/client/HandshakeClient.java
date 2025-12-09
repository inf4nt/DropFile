package com.evolution.dropfiledaemon.client;

import com.evolution.dropfile.common.dto.HandshakeChallengeRequestBodyDTO;
import com.evolution.dropfile.common.dto.HandshakeRequestBodyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

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
    public HttpResponse<byte[]> getChallenge(URI handshakeNodeAddressURI, String challenge) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeNodeAddressURI.resolve("/handshake/challenge"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new HandshakeChallengeRequestBodyDTO(challenge))
                ))
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getHandshake(URI handshakeNodeAddressURI, String fingerprint) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeNodeAddressURI.resolve("/handshake/trust/")
                        .resolve(fingerprint)
                )
                .GET()
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshakeRequest(URI currentAddressURI,
                                                 URI handshakeNodeAddressURI,
                                                 byte[] publicKey) {
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeNodeAddressURI.resolve("/handshake/request"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new HandshakeRequestBodyDTO(
                                currentAddressURI,
                                Base64.getEncoder().encodeToString(publicKey)
                        ))
                ))
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }
}
