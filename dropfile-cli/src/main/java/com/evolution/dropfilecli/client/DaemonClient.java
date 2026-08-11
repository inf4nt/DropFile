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

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DaemonClient {

    private final HttpClient httpClient;

    private final CliApplicationProperties cliApplicationProperties;

    private final DaemonSecretsStore daemonSecretsStore;

    private final ObjectMapper objectMapper;

    public HttpResponse<byte[]> handshake(URI address, String key, boolean force) {
        return sendPost("/api/handshake", new ApiHandshakeRequestDTO(address.toString(), key, force));
    }

    public HttpResponse<byte[]> handshakeReconnect(URI address) {
        return sendPost("/api/handshake/reconnect", new ApiHandshakeReconnectRequestDTO(address.toString()));
    }

    public HttpResponse<byte[]> handshakeCurrentReconnect() {
        return sendPost("/api/handshake/current/reconnect");
    }

    public HttpResponse<byte[]> handshakeDisconnect(String fingerprint) {
        return sendPost(CommonUtils.joinPaths("/api/handshake/disconnect/fingerprint", fingerprint));
    }

    public HttpResponse<byte[]> handshakeDisconnectCurrent() {
        return sendPost("/api/handshake/disconnect/current");
    }

    public HttpResponse<byte[]> handshakeDisconnectAll() {
        return sendPost("/api/handshake/disconnect/all");
    }

    public HttpResponse<byte[]> handshakeRevoke(String fingerprint) {
        return sendPost(CommonUtils.joinPaths("/api/handshake/revoke/fingerprint", fingerprint));
    }

    public HttpResponse<byte[]> handshakeRevokeAll() {
        return sendPost("/api/handshake/revoke/all");
    }

    public HttpResponse<byte[]> getTrustIn() {
        return sendGet("/api/handshake/trust/in");
    }

    public HttpResponse<byte[]> getTrustOut() {
        return sendGet("/api/handshake/trust/out");
    }

    public HttpResponse<byte[]> getTrustLatest() {
        return sendGet("/api/handshake/trust/out/latest");
    }

    public HttpResponse<byte[]> connectionsTraffic() {
        return sendGet("/api/connections/traffic");
    }

    public HttpResponse<byte[]> connectionsBrowseLs(List<String> ids) {
        return sendPost("/api/connections/browse/ls", new ApiConnectionsBrowseLsRequestDTO(ids));
    }

    public HttpResponse<byte[]> connectionsBrowseGet(String id, String filename) {
        return sendPost("/api/connections/browse/get", new ApiConnectionsBrowseGetRequestDTO(id, filename));
    }

    public HttpResponse<byte[]> connectionsShareLs() {
        return sendGet("/api/connections/share/ls");
    }

    public HttpResponse<byte[]> connectionsShareAdd(String resourcePath, String alias) {
        return sendPost("/api/connections/share/add", new ApiConnectionsShareAddRequestDTO(resourcePath, alias));
    }

    public HttpResponse<byte[]> connectionsShareRm(String id) {
        return sendDelete(CommonUtils.joinPaths("/api/connections/share/rm", id));
    }

    public HttpResponse<byte[]> connectionsShareRmAll() {
        return sendDelete("/api/connections/share/rm-all");
    }

    public HttpResponse<byte[]> connectionsAccessGenerate(boolean permanent) {
        return sendPost("/api/connections/access/generate", new ApiConnectionsAccessGenerateRequestDTO(permanent));
    }

    public HttpResponse<byte[]> connectionsAccessLs() {
        return sendGet("/api/connections/access/ls");
    }

    public HttpResponse<byte[]> connectionsAccessRm(String id) {
        return sendDelete(CommonUtils.joinPaths("/api/connections/access/rm", id));
    }

    public HttpResponse<byte[]> connectionsAccessRmAll() {
        return sendDelete("/api/connections/access/rm-all");
    }

    public HttpResponse<byte[]> downloadLs(ApiDownloadLsDTO.Status status, Integer limit) {
        return sendPost("/api/download/ls", new ApiDownloadLsDTO.Request(status, limit));
    }

    public HttpResponse<byte[]> downloadStop(String operation) {
        return sendPost(CommonUtils.joinPaths("/api/download/stop", operation));
    }

    public HttpResponse<byte[]> downloadStopAll() {
        return sendPost("/api/download/stop-all");
    }

    public HttpResponse<byte[]> downloadRm(String operationId) {
        return sendDelete(CommonUtils.joinPaths("/api/download/rm", operationId));
    }

    public HttpResponse<byte[]> downloadRmAll() {
        return sendDelete("/api/download/rm-all");
    }

    public HttpResponse<byte[]> quickShareAdd(String resourcePath,
                                              boolean singleUse,
                                              boolean secure,
                                              String secret) {
        return sendPost("/api/quick-share/add", new ApiQuickShareAddRequestDTO(resourcePath, singleUse, secure, secret));
    }

    public HttpResponse<byte[]> quickShareLs() {
        return sendGet("/api/quick-share/ls");
    }

    public HttpResponse<byte[]> quickShareShow(String id) {
        return sendGet(CommonUtils.joinPaths("/api/quick-share/ls", id));
    }

    public HttpResponse<byte[]> quickShareRm(String id) {
        return sendDelete(CommonUtils.joinPaths("/api/quick-share/rm", id));
    }

    public HttpResponse<byte[]> quickShareRmAll() {
        return sendDelete("/api/quick-share/rm-all");
    }

    public HttpResponse<byte[]> daemonInfo() {
        return sendGet("/api/daemon/info");
    }

    public HttpResponse<byte[]> daemonShutdown() {
        return sendPost("/api/daemon/shutdown");
    }

    public HttpResponse<byte[]> daemonCacheReset() {
        return sendPost("/api/daemon/cache-reset");
    }

    public HttpResponse<byte[]> daemonGarbageCollector() {
        return sendPost("/api/daemon/garbage-collector");
    }

    private HttpResponse<byte[]> sendGet(String path) {
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("GET", path, HttpRequest.BodyPublishers.noBody());
        return execute(httpRequestBuilder);
    }

    private HttpResponse<byte[]> sendPost(String path) {
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("POST", path, HttpRequest.BodyPublishers.noBody());
        return execute(httpRequestBuilder);
    }

    @SneakyThrows
    private HttpResponse<byte[]> sendPost(String path, Object bodyDTO) {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(bodyDTO);
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("POST", path, HttpRequest.BodyPublishers.ofByteArray(jsonBytes));
        httpRequestBuilder.header("Content-Type", "application/json");
        return execute(httpRequestBuilder);
    }

    private HttpResponse<byte[]> sendDelete(String path) {
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("DELETE", path, HttpRequest.BodyPublishers.noBody());
        return execute(httpRequestBuilder);
    }

    @SneakyThrows
    private HttpResponse<byte[]> execute(HttpRequest.Builder requestBuilder) {
        HttpRequest httpRequest = requestBuilder.build();
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (ConnectException e) {
            String host = httpRequest.uri().getHost();
            int port = httpRequest.uri().getPort();
            throw new IOException("Daemon is not running or unreachable. Check daemon host %s and port %s"
                    .formatted(host, port), e);
        }
    }

    private HttpRequest.Builder HttpRequestBuilder(String method,
                                                   String path,
                                                   HttpRequest.BodyPublisher bodyPublisher) {
        URI uri = CommonUtils.toURI(cliApplicationProperties.daemonHost, cliApplicationProperties.daemonPort)
                .resolve(path);
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", getDaemonAuthorizationToken())
                .method(method, bodyPublisher);
    }

    private String getDaemonAuthorizationToken() {
        DaemonSecrets daemonSecrets = daemonSecretsStore.getRequired();
        String daemonToken = Objects.requireNonNull(daemonSecrets.daemonToken());
        return "Bearer " + daemonToken;
    }
}
