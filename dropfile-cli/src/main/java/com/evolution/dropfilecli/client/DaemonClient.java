package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfilecli.config.CliApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.NoSuchElementException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DaemonClient {

    private final HttpClient httpClient;

    private final CliApplicationProperties cliApplicationProperties;

    private final DaemonSecretStore daemonSecretStore;

    private final ObjectMapper objectMapper;

    public HttpResponse<byte[]> handshake(URI address, String key, boolean force) throws IOException {
        return sendPost("/api/handshake", new ApiHandshakeRequestDTO(address.toString(), key, force));
    }

    public HttpResponse<byte[]> handshakeReconnect(URI address) throws IOException {
        return sendPost("/api/handshake/reconnect", new ApiHandshakeReconnectRequestDTO(address.toString()));
    }

    public HttpResponse<byte[]> handshakeCurrentReconnect() throws IOException {
        return sendPost("/api/handshake/current/reconnect");
    }

    public HttpResponse<byte[]> handshakeDisconnect(String fingerprint) throws IOException {
        return sendPost(CommonUtils.joinPaths("/api/handshake/disconnect/fingerprint", fingerprint));
    }

    public HttpResponse<byte[]> handshakeDisconnectCurrent() throws IOException {
        return sendPost("/api/handshake/disconnect/current");
    }

    public HttpResponse<byte[]> handshakeDisconnectAll() throws IOException {
        return sendPost("/api/handshake/disconnect/all");
    }

    public HttpResponse<byte[]> handshakeRevoke(String fingerprint) throws IOException {
        return sendPost(CommonUtils.joinPaths("/api/handshake/revoke/fingerprint", fingerprint));
    }

    public HttpResponse<byte[]> handshakeRevokeAll() throws IOException {
        return sendPost("/api/handshake/revoke/all");
    }

    public HttpResponse<byte[]> getTrustIn() throws IOException {
        return sendGet("/api/handshake/trust/in");
    }

    public HttpResponse<byte[]> getTrustOut() throws IOException {
        return sendGet("/api/handshake/trust/out");
    }

    public HttpResponse<byte[]> getTrustLatest() throws IOException {
        return sendGet("/api/handshake/trust/out/latest");
    }

    public HttpResponse<byte[]> connectionsTraffic() throws IOException {
        return sendGet("/api/connections/traffic");
    }

    public HttpResponse<byte[]> connectionsBrowseLs(List<String> ids) throws IOException {
        return sendPost("/api/connections/browse/ls", new ApiConnectionsBrowseLsRequestDTO(ids));
    }

    public HttpResponse<byte[]> connectionsBrowseGet(String id, String filename) throws IOException {
        return sendPost("/api/connections/browse/get", new ApiConnectionsBrowseGetRequestDTO(id, filename));
    }

    public HttpResponse<byte[]> connectionsShareLs() throws IOException {
        return sendGet("/api/connections/share/ls");
    }

    public HttpResponse<byte[]> connectionsShareAdd(String resourcePath, String alias) throws IOException {
        return sendPost("/api/connections/share/add", new ApiConnectionsShareAddRequestDTO(resourcePath, alias));
    }

    public HttpResponse<byte[]> connectionsShareRm(Set<String> ids) throws IOException {
        return sendDelete("/api/connections/share/rm", ids);
    }

    public HttpResponse<byte[]> connectionsShareRmAll() throws IOException {
        return sendDelete("/api/connections/share/rm-all");
    }

    public HttpResponse<byte[]> connectionsAccessGenerate(boolean permanent) throws IOException {
        return sendPost("/api/connections/access/generate", new ApiConnectionsAccessGenerateRequestDTO(permanent));
    }

    public HttpResponse<byte[]> connectionsAccessLs() throws IOException {
        return sendGet("/api/connections/access/ls");
    }

    public HttpResponse<byte[]> connectionsAccessRm(String id) throws IOException {
        return sendDelete(CommonUtils.joinPaths("/api/connections/access/rm", id));
    }

    public HttpResponse<byte[]> connectionsAccessRmAll() throws IOException {
        return sendDelete("/api/connections/access/rm-all");
    }

    public HttpResponse<byte[]> downloadLs(ApiDownloadLsDTO.Status status, Integer limit) throws IOException {
        return sendPost("/api/download/ls", new ApiDownloadLsDTO.Request(status, limit));
    }

    public HttpResponse<byte[]> downloadStop(Set<String> startWithOperationIds) throws IOException {
        return sendPost("/api/download/stop", startWithOperationIds);
    }

    public HttpResponse<byte[]> downloadStopAll() throws IOException {
        return sendPost("/api/download/stop-all");
    }

    public HttpResponse<byte[]> downloadRm(Set<String> startWithOperationIds, boolean force) throws IOException {
        return sendDelete("/api/download/rm", new ApiDownloadRmRequest(startWithOperationIds, force));
    }

    public HttpResponse<byte[]> downloadRmAll() throws IOException {
        return sendDelete("/api/download/rm-all");
    }

    public HttpResponse<byte[]> quickShareAdd(String resourcePath,
                                              boolean singleUse,
                                              boolean secure,
                                              String secret) throws IOException {
        return sendPost("/api/quickshare/add", new ApiQuickShareAddRequestDTO(resourcePath, singleUse, secure, secret));
    }

    public HttpResponse<byte[]> quickShareLs() throws IOException {
        return sendGet("/api/quickshare/ls");
    }

    public HttpResponse<byte[]> quickShareShow(String id) throws IOException {
        return sendGet(CommonUtils.joinPaths("/api/quickshare/ls", id));
    }

    public HttpResponse<byte[]> quickShareRm(Set<String> idCriteria) throws IOException {
        return sendDelete("/api/quickshare/rm", idCriteria);
    }

    public HttpResponse<byte[]> quickShareRmAll() throws IOException {
        return sendDelete("/api/quickshare/rm-all");
    }

    public HttpResponse<byte[]> daemonPing() throws IOException {
        return sendGet("/api/daemon/ping");
    }

    public HttpResponse<byte[]> daemonInfo() throws IOException {
        return sendGet("/api/daemon/info");
    }

    public HttpResponse<byte[]> daemonShutdown() throws IOException {
        return sendPost("/api/daemon/shutdown");
    }

    public HttpResponse<byte[]> daemonCacheReset() throws IOException {
        return sendPost("/api/daemon/cache-reset");
    }

    public HttpResponse<byte[]> daemonGarbageCollector() throws IOException {
        return sendPost("/api/daemon/garbage-collector");
    }

    private HttpResponse<byte[]> sendGet(String path) throws IOException {
        HttpRequest httpRequest = HttpRequestBuilder("GET", path, HttpRequest.BodyPublishers.noBody())
                .build();
        return execute(httpRequest);
    }

    private HttpResponse<byte[]> sendPost(String path) throws IOException {
        HttpRequest httpRequest = HttpRequestBuilder("POST", path, HttpRequest.BodyPublishers.noBody())
                .build();
        return execute(httpRequest);
    }

    private HttpResponse<byte[]> sendPost(String path, Object bodyDTO) throws IOException {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(bodyDTO);
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("POST", path, HttpRequest.BodyPublishers.ofByteArray(jsonBytes));
        httpRequestBuilder.header("Content-Type", "application/json");
        HttpRequest httpRequest = httpRequestBuilder.build();
        return execute(httpRequest);
    }

    private HttpResponse<byte[]> sendDelete(String path) throws IOException {
        HttpRequest httpRequest = HttpRequestBuilder("DELETE", path, HttpRequest.BodyPublishers.noBody())
                .build();
        return execute(httpRequest);
    }

    private HttpResponse<byte[]> sendDelete(String path, Object body) throws IOException {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(body);
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("DELETE", path, HttpRequest.BodyPublishers.ofByteArray(jsonBytes));
        httpRequestBuilder.header("Content-Type", "application/json");
        HttpRequest httpRequest = httpRequestBuilder.build();
        return execute(httpRequest);
    }

    private HttpResponse<byte[]> execute(HttpRequest httpRequest) throws IOException {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Daemon client call execution interrupted: %s %s"
                    .formatted(httpRequest.method(), httpRequest.uri()), e);
        } catch (ConnectException e) {
            String message = "Is daemon running? Daemon is not running or unreachable %s %s"
                    .formatted(httpRequest.method(), httpRequest.uri());
            throw new ConnectException(message);
        } catch (IOException e) {
            throw new IOException("I/O error during daemon client call %s %s"
                    .formatted(httpRequest.method(), httpRequest.uri()), e);
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
        DaemonSecret daemonSecret = daemonSecretStore.get()
                .orElseThrow(() -> new NoSuchElementException("Is daemon running? Unable to get daemon token from the store. It might be daemon has not initialized yet"));
        return "Bearer " + daemonSecret.daemonToken();
    }
}
