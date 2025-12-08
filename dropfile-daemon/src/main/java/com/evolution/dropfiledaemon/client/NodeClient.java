package com.evolution.dropfiledaemon.client;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class NodeClient {

    private final HttpClient httpClient;

    @Autowired
    public NodeClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @SneakyThrows
    public HttpResponse<String> nodePing(URI nodeAddressURI, String token) {
        URI addressURI = nodeAddressURI
                .resolve("/node/ping");

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(addressURI)
                .header("X-Encrypted-Token", token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
