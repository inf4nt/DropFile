package com.evolution.dropfilecli.client;

import com.evolution.dropfilecli.configuration.DropFileCliConfiguration;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class DaemonHttpClient {

    private final HttpClient httpClient;

    private final DropFileCliConfiguration dropFileCliConfiguration;

    @Autowired
    public DaemonHttpClient(HttpClient httpClient, DropFileCliConfiguration dropFileCliConfiguration) {
        this.httpClient = httpClient;
        this.dropFileCliConfiguration = dropFileCliConfiguration;
    }

    @SneakyThrows
    public HttpResponse<Void> connect(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/connect"))
                .POST(HttpRequest.BodyPublishers.ofString(uri.toString()))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<String> online() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/connect/online"))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<Void> disconnect() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/disconnect"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<String> status() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/connect/status"))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<String> shutdown() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/shutdown"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<String> ping() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/ping"))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<String> getFiles(String filePath) {
        String queryFilePath = encode(filePath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/files?filePath=" + queryFilePath))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<InputStream> download(String filePath) {
        String queryFilePath = encode(filePath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/download?filePath=" + queryFilePath))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    @SneakyThrows
    public HttpResponse<String> getNodes() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dropFileCliConfiguration.getDaemonURI() + "/daemon/node"))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String encode(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }
}
