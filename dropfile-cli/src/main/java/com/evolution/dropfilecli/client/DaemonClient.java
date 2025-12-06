package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.common.dto.ConnectionsConnectionDTO;
import com.evolution.dropfile.common.dto.HandshakeApiRequestDTO;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

@Component
public class DaemonClient {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final DropFileAppConfig.DropFileCliAppConfig cliAppConfig;

    private final DropFileSecretsConfig secretsConfig;

    @Autowired
    public DaemonClient(HttpClient httpClient,
                        ObjectMapper objectMapper,
                        DropFileAppConfig.DropFileCliAppConfig cliAppConfig,
                        DropFileSecretsConfig secretsConfig) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cliAppConfig = cliAppConfig;
        this.secretsConfig = secretsConfig;
    }

    @SneakyThrows
    public HttpResponse<Void> pingDaemon() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort());
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
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort());
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
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort());
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

    @SneakyThrows
    public HttpResponse<Void> shutdown() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort());
        URI daemonShutdownUri = daemonURI.resolve("/api/shutdown");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonShutdownUri)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshakeRequest(String nodeAddress) {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/request");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        byte[] payload = objectMapper.writeValueAsBytes(new HandshakeApiRequestDTO(nodeAddress));

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String getDaemonAuthorizationToken() {
        String daemonToken = Objects.requireNonNull(secretsConfig.getDaemonToken());
        return "Bearer " + daemonToken;
//        return "fake";
    }
}
