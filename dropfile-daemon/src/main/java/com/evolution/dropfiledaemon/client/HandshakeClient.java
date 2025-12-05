package com.evolution.dropfiledaemon.client;

import com.evolution.dropfile.configuration.dto.HandshakeChallengeRequestDTO;
import com.evolution.dropfile.configuration.dto.HandshakeChallengeResponseDTO;
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
        URI handshakeURI = handshakeNodeAddressURI
                .resolve("/handshake/challenge");
        byte[] payload = objectMapper.writeValueAsBytes(new HandshakeChallengeRequestDTO(challenge));
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getHandshake(URI handshakeNodeAddressURI, String fingerprint) {
        URI handshakeURI = handshakeNodeAddressURI
                .resolve("/handshake/trust/" + fingerprint);
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeURI)
                .GET()
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<Void> handshakeRequest(URI handshakeNodeAddressURI,
                                               byte[] currentNodePublicKey) {
        URI handshakeURI = handshakeNodeAddressURI.resolve("/handshake/request");

        String currentPublicKeyBase64 = Base64.getEncoder().encodeToString(currentNodePublicKey);
        byte[] payload = objectMapper.writeValueAsBytes(new HandshakeRequestDTO(currentPublicKeyBase64));

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }
}
