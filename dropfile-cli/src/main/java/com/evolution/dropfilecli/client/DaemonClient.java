package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.HandshakeApiRequestBodyDTO;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
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
    public HttpResponse<byte[]> getDaemonInfo() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/info");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<Void> pingDaemon() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/ping");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
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
    public HttpResponse<String> handshakeRequest(String nodeAddress, Integer timeout) {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/request");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        byte[] payload = objectMapper.writeValueAsBytes(new HandshakeApiRequestBodyDTO(nodeAddress, timeout));

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getIncomingRequests() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/request/incoming");
        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getOutgoingRequests() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/request/outgoing");
        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getTrustIn() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/trust/in");
        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getTrustOut() {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/trust/out");
        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> trust(String fingerprint) {
        URI daemonURI = CommonUtils.toURI(cliAppConfig.getDaemonHost(), cliAppConfig.getDaemonPort())
                .resolve("/api/handshake/trust/")
                .resolve(fingerprint);
        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String getDaemonAuthorizationToken() {
        String daemonToken = Objects.requireNonNull(secretsConfig.getDaemonToken());
        return "Bearer " + daemonToken;
//        return "fake";
    }
}
