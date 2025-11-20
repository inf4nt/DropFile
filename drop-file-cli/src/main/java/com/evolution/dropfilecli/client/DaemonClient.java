package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ConnectionsConnectionDTO;
import com.evolution.dropfile.common.dto.ConnectionsOnline;
import com.evolution.dropfilecli.configuration.DropFileCliConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class DaemonClient {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final DropFileCliConfiguration dropFileCliConfiguration;

    @Autowired
    public DaemonClient(HttpClient httpClient,
                        DropFileCliConfiguration dropFileCliConfiguration,
                        ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.dropFileCliConfiguration = dropFileCliConfiguration;
    }

    @SneakyThrows
    public ConnectionsOnline getOnlineConnections() {
        URI daemonURI = CommonUtils.toURI(dropFileCliConfiguration.getDaemonAddress());
        URI daemonConnectionUri = daemonURI.resolve("/daemon/connections/online");

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonConnectionUri)
                .GET()
                .build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = httpResponse.body();
        return objectMapper.readValue(body, ConnectionsOnline.class);
    }

    @SneakyThrows
    public HttpResponse<String> connect(String address) {
        ConnectionsConnectionDTO connectionsConnectionDTO = new ConnectionsConnectionDTO(
                address
        );
        String bodyJson = objectMapper.writeValueAsString(connectionsConnectionDTO);
        URI daemonURI = CommonUtils.toURI(dropFileCliConfiguration.getDaemonAddress());
        URI daemonConnectionUri = daemonURI.resolve("/daemon/connections/connect");

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonConnectionUri)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
