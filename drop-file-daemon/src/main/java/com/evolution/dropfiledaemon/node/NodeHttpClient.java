package com.evolution.dropfiledaemon.node;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class NodeHttpClient {

    private final HttpClient httpClient;

    @Autowired
    private Environment env;

    @Autowired
    public NodeHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @SneakyThrows
    public HttpResponse<Void> connect(URI uri) {
        URI uriNodeConnect = uri.resolve("/daemon/node/connect");
        String serverPort = env.getProperty("server.port", "8080");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriNodeConnect)
                .POST(HttpRequest.BodyPublishers.ofString(serverPort))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<String> getFiles(URI uri, String filePath) {
        String queryFilePath = encode(filePath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "/daemon/node/files?filePath=" + queryFilePath))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<InputStream> download(URI uri, String filePath) {
        String queryFilePath = encode(filePath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "/daemon/node/download?filePath=" + queryFilePath))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private String encode(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }
}
