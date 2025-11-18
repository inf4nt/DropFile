package com.evolution.dropfilecli.client;

import com.evolution.dropfilecli.CommandSession;
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

    private final CommandSession commandSession;

    @Autowired
    public DaemonHttpClient(HttpClient httpClient, CommandSession commandSession) {
        this.httpClient = httpClient;
        this.commandSession = commandSession;
    }

    @SneakyThrows
    public HttpResponse<String> shutdown() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commandSession.getPeerIp() + "/daemon/shutdown"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<String> ping() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commandSession.getPeerIp() + "/daemon/ping"))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<String> getFiles(String filePath) {
        String queryFilePath = encode(filePath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commandSession.getPeerIp() + "/daemon/file/list?filePath=" + queryFilePath))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<InputStream> download(String filePath) {
        String queryFilePath = encode(filePath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commandSession.getPeerIp() + "/daemon/file/download?filePath=" + queryFilePath))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private String encode(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }
}
