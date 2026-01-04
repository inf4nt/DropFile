package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.AccessKeyGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiFileAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeReconnectRequestDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeRequestDTO;
import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.secret.SecretsConfig;
import com.evolution.dropfile.configuration.secret.SecretsConfigStore;
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

    private final AppConfigStore appConfigStore;

    private final SecretsConfigStore secretsConfigStore;

    @Autowired
    public DaemonClient(HttpClient httpClient,
                        ObjectMapper objectMapper,
                        AppConfigStore appConfigStore,
                        SecretsConfigStore secretsConfigStore) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appConfigStore = appConfigStore;
        this.secretsConfigStore = secretsConfigStore;
    }

    @SneakyThrows
    public HttpResponse<byte[]> getConnectionFiles() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/files/ls");

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
    public HttpResponse<byte[]> connectionsDownloadFile(String id) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/files/download/")
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
    public HttpResponse<byte[]> getFiles() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/files");

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
    public HttpResponse<byte[]> addFile(String alias, String absoluteFilePath) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/files");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(
                                new ApiFileAddRequestDTO(alias, absoluteFilePath)
                        )
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> removeFile(String id) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/files/")
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
    public HttpResponse<byte[]> removeAllFiles() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/files");

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
    public HttpResponse<byte[]> generateAccessKeys(boolean permanent) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(
                                new AccessKeyGenerateRequestDTO(permanent)
                        )
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getAccessKeys() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access");

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
    public HttpResponse<Void> revokeAccessKey(String id) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access/")
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
    public HttpResponse<Void> revokeAllAccessKeys() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/connections/access");

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
    public HttpResponse<byte[]> getDaemonInfo() {
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
    public HttpResponse<Void> pingDaemon() {
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
    public HttpResponse<Void> shutdown() {
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
