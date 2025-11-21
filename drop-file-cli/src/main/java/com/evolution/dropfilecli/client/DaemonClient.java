package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ConnectionsConnectionDTO;
import com.evolution.dropfilecli.configuration.CliConfig;
import com.evolution.dropfilecli.configuration.DaemonConfig;
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

    private final CliConfig cliConfig;

    private final DaemonConfig daemonConfig;

    @Autowired
    public DaemonClient(HttpClient httpClient,
                        CliConfig cliConfig,
                        ObjectMapper objectMapper, DaemonConfig daemonConfig) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cliConfig = cliConfig;
        this.daemonConfig = daemonConfig;
    }

    @SneakyThrows
    public HttpResponse<Void> pingDaemon() {
        URI daemonURI = CommonUtils.toURI(cliConfig.getDaemonAddress());
        URI daemonPingUri = daemonURI.resolve("/api/ping");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonPingUri)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<String> getOnlineConnections() {
        URI daemonURI = CommonUtils.toURI(cliConfig.getDaemonAddress());
        URI daemonConnectionUri = daemonURI.resolve("/api/connections/online");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonConnectionUri)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<String> connect(String address) {
        ConnectionsConnectionDTO connectionsConnectionDTO = new ConnectionsConnectionDTO(
                address
        );
        String bodyJson = objectMapper.writeValueAsString(connectionsConnectionDTO);
        URI daemonURI = CommonUtils.toURI(cliConfig.getDaemonAddress());
        URI daemonConnectionUri = daemonURI.resolve("/api/connections/connect");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonConnectionUri)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getDaemonAuthorizationToken() {
        return "Bearer " + daemonConfig.getToken();
//        return "fake";
    }
}
