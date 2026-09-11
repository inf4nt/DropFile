package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfilecli.config.CliApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class DaemonClient {

    private static final String HEADER_TIMEOUT = "X-Timeout-Ms";

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

    public HttpResponse<byte[]> handshakeDisconnect(CriteriaEnvelope fingerprintCriteriaEnvelope) throws IOException {
        return sendPost(CommonUtils.joinPaths("/api/handshake/disconnect/fingerprint"), fingerprintCriteriaEnvelope);
    }

    public HttpResponse<byte[]> handshakeDisconnectCurrent() throws IOException {
        return sendPost("/api/handshake/disconnect/current");
    }

    public HttpResponse<byte[]> handshakeDisconnectAll() throws IOException {
        return sendPost("/api/handshake/disconnect/all");
    }

    public HttpResponse<byte[]> handshakeRevoke(CriteriaEnvelope fingerprintCriteriaEnvelope) throws IOException {
        return sendPost(CommonUtils.joinPaths("/api/handshake/revoke/fingerprint"), fingerprintCriteriaEnvelope);
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

    public HttpResponse<byte[]> connectionsTunnelPing() throws IOException {
        return sendGet("/api/connections/tunnel/ping");
    }

    public HttpResponse<byte[]> connectionsBrowseLs(Collection<CriteriaEnvelope> criteriaEnvelopes) throws IOException {
        return sendPost("/api/connections/browse/ls", new ApiConnectionsBrowseLsRequestDTO(criteriaEnvelopes));
    }

    public HttpResponse<byte[]> connectionsBrowseGet(CriteriaEnvelope fileIdCriteriaEnvelope, String filename) throws IOException {
        return sendPost("/api/connections/browse/get", new ApiConnectionsBrowseGetRequestDTO(fileIdCriteriaEnvelope, filename));
    }

    public HttpResponse<byte[]> connectionsShareLs() throws IOException {
        return sendGet("/api/connections/share/ls");
    }

    public HttpResponse<byte[]> connectionsShareAdd(String resourcePath, String alias, Long timeout) throws IOException {
        return sendPost("/api/connections/share/add", new ApiConnectionsShareAddRequestDTO(resourcePath, alias), timeout);
    }

    public HttpResponse<byte[]> connectionsShareRm(Collection<CriteriaEnvelope> shareFileIdCriteriaEnvelopes) throws IOException {
        return sendDelete("/api/connections/share/rm", shareFileIdCriteriaEnvelopes);
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

    public HttpResponse<byte[]> connectionsAccessRm(Collection<CriteriaEnvelope> accessIdCriteriaEnvelopes) throws IOException {
        return sendDelete("/api/connections/access/rm", accessIdCriteriaEnvelopes);
    }

    public HttpResponse<byte[]> connectionsAccessRmAll() throws IOException {
        return sendDelete("/api/connections/access/rm-all");
    }

    public HttpResponse<byte[]> connectionsDownloadLs(ApiDownloadLsDTO.Status status, Integer limit) throws IOException {
        return sendPost("/api/connections/download/ls", new ApiDownloadLsDTO.Request(status, limit));
    }

    public HttpResponse<byte[]> connectionsDownloadStop(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) throws IOException {
        return sendPost("/api/connections/download/stop", operationIdCriteriaEnvelopes);
    }

    public HttpResponse<byte[]> connectionsDownloadKill(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) throws IOException {
        return sendPost("/api/connections/download/kill", operationIdCriteriaEnvelopes);
    }

    public HttpResponse<byte[]> connectionsDownloadKillAll() throws IOException {
        return sendPost("/api/connections/download/kill-all");
    }

    public HttpResponse<byte[]> connectionsDownloadStopAll() throws IOException {
        return sendPost("/api/connections/download/stop-all");
    }

    public HttpResponse<byte[]> connectionsDownloadRm(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) throws IOException {
        return sendDelete("/api/connections/download/rm", new ApiDownloadRmRequest(operationIdCriteriaEnvelopes));
    }

    public HttpResponse<byte[]> connectionsDownloadRmAll() throws IOException {
        return sendDelete("/api/connections/download/rm-all");
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

    public HttpResponse<byte[]> quickShareShow(CriteriaEnvelope idCriteriaEnvelope) throws IOException {
        return sendPost("/api/quickshare/show", idCriteriaEnvelope);
    }

    public HttpResponse<byte[]> quickShareRm(Collection<CriteriaEnvelope> idCriteriaEnvelope) throws IOException {
        return sendDelete("/api/quickshare/rm", idCriteriaEnvelope);
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
        return sendPost(path, bodyDTO, null);
    }

    private HttpResponse<byte[]> sendPost(String path, Object bodyDTO, @Nullable Long timeout) throws IOException {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(bodyDTO);
        HttpRequest.Builder httpRequestBuilder = HttpRequestBuilder("POST", path, HttpRequest.BodyPublishers.ofByteArray(jsonBytes));
        httpRequestBuilder.header("Content-Type", "application/json");
        if (timeout != null) {
            httpRequestBuilder.header(HEADER_TIMEOUT, String.valueOf(timeout));
            httpRequestBuilder.timeout(Duration.ofMillis(timeout));
        }
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
            String message = "Is daemon running? Daemon is not running or unreachable %s %s. Execute $ dropf daemon start"
                    .formatted(httpRequest.method(), httpRequest.uri());
            throw new ConnectException(message);
        } catch (HttpConnectTimeoutException e) {
            throw new IOException("HTTP connect timed out during daemon client call %s %s"
                    .formatted(httpRequest.method(), httpRequest.uri()), e);
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
                .timeout(Duration.ofMillis(cliApplicationProperties.daemonClientHttpRequestTimeoutMillis))
                .method(method, bodyPublisher);
    }

    private String getDaemonAuthorizationToken() {
        DaemonSecret daemonSecret = daemonSecretStore.get()
                .orElseThrow(() -> new NoSuchElementException("Is daemon running? Unable to get daemon token from the store. It might be daemon has not initialized yet. Execute $ dropf daemon start"));
        return "Bearer " + daemonSecret.daemonToken();
    }
}
