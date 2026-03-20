package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfilecli.config.CliApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class DaemonClient {

    private final HttpClient httpClient;

    private final CliApplicationProperties cliApplicationProperties;

    private final DaemonSecretsStore daemonSecretsStore;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public HttpResponse<byte[]> connectionsDisconnect(String fingerprint) {
        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/disconnect/fingerprint/")
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

    @SneakyThrows
    public HttpResponse<byte[]> connectionsDisconnectCurrent() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/disconnect/current");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsDisconnectAll() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/disconnect/all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsRevoke(String fingerprint) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/revoke/fingerprint/")
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

    @SneakyThrows
    public HttpResponse<byte[]> connectionsRevokeAll() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/revoke/all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionShareLs(List<String> ids) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/share/ls");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(
                                new ApiConnectionsShareLsRequestDTO(ids)
                        )
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsShareDownload(String id, String filename) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/share/download");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(
                        new ApiConnectionsShareDownloadRequestDTO(id, filename)
                )))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsShareCat(String id) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/share/cat/")
                .resolve(id);

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
    public HttpResponse<byte[]> shareLs() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/share/ls");

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
    public HttpResponse<byte[]> shareAdd(String alias, String absoluteFilePath) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/share/rm/")
                .resolve(id);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> shareRmAll() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/share/rm-all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsAccessGenerate(boolean permanent) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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
    public HttpResponse<byte[]> connectionsAccessRm(String id) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/access/rm/")
                .resolve(id);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> connectionsAccessRmAll() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/connections/access/rm-all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> daemonInfo() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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
    public HttpResponse<byte[]> daemonShutdown() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/daemon/shutdown");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> daemonCacheReset() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/daemon/cache-reset");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> handshake(URI address,
                                          String key) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
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

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/handshake/status");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> downloadLs(ApiDownloadLsDTO.Status status, Integer limit) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/download/ls");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new ApiDownloadLsDTO.Request(
                                status,
                                limit
                        ))
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> downloadStop(String operation) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/download/stop/")
                .resolve(operation);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> downloadStopAll() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/download/stop-all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> downloadRm(String operationId) {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/download/rm/")
                .resolve(operationId);

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @SneakyThrows
    public HttpResponse<byte[]> downloadRmAll() {

        URI daemonURI = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve("/api/download/rm-all");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String getDaemonAuthorizationToken() {
        DaemonSecrets daemonSecrets = daemonSecretsStore.getRequired();
        String daemonToken = Objects.requireNonNull(daemonSecrets.daemonToken());
        return "Bearer " + daemonToken;
    }
}
