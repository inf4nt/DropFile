package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.secret.SecretsConfig;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
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

    private final AppConfigStore appConfigStore;

    private final SecretsConfigStore secretsConfigStore;

    private final ObjectMapper objectMapper;

    @Autowired
    public DaemonClient(HttpClient httpClient,
                        AppConfigStore appConfigStore,
                        SecretsConfigStore secretsConfigStore,
                        ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.appConfigStore = appConfigStore;
        this.secretsConfigStore = secretsConfigStore;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionShareLs() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/share/ls");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsShareDownload(String id, String filename, boolean rewrite) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/share/download");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(
                        new ApiConnectionsShareDownloadRequestDTO(id, filename, rewrite)
                )))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsShareCat(String id) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/share/cat/")
                .resolve(id);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> shareLs() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/share/ls");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> shareAdd(String alias, String absoluteFilePath) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/share/add");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(
                                new ApiShareAddRequestDTO(alias, absoluteFilePath)
                        )
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> shareRm(String id) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/share/rm/")
                .resolve(id);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> shareRmAll() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/share/rm-all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsAccessGenerate(boolean permanent) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access/generate");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(
                                new ApiConnectionsAccessGenerateRequestDTO(permanent)
                        )
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsAccessLs() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access/ls");

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
    public HttpResponse<Void> connectionsAccessRm(String id) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access/rm/")
                .resolve(id);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<Void> connectionsAccessRmAll() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access/rm-all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<byte[]> daemonInfo() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/daemon/info");

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
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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
    public HttpResponse<byte[]> getTrustLatest() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/trust/out/latest");

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
    public HttpResponse<Void> daemonPing() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/daemon/ping");

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
    public HttpResponse<Void> daemonShutdown() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/daemon/shutdown");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshake(URI address,
                                          String key) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new ApiHandshakeRequestDTO(
                                address.toString(),
                                key
                        ))
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshakeReconnect(URI address) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/reconnect");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new ApiHandshakeReconnectRequestDTO(
                                address.toString()
                        ))
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshakeStatus() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/status");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String getDaemonAuthorizationToken() {
        SecretsConfig secretsConfig = secretsConfigStore.getRequired();

        String daemonToken = Objects.requireNonNull(secretsConfig.daemonToken());
        return "Bearer " + daemonToken;
//        return "fake";
    }
}
