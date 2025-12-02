package com.evolution.dropfiledaemon.client;

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

    @Autowired
    public HandshakeClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshakeRequest(URI nodeAddressURI, byte[] requestBody) {
        URI handshakeURI = nodeAddressURI.resolve("/handshake/request");

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshakeRequestApproved(URI nodeAddressURI,
                                                         byte[] requestBody) {
        URI handshakeRequestApprovedURI = nodeAddressURI.resolve("/handshake/request/approved");

        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .uri(handshakeRequestApprovedURI)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
    }
}
