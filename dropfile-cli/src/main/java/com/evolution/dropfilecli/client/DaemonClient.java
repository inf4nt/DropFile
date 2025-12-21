package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.DaemonSetPublicAddressRequestBodyDTO;
import com.evolution.dropfile.common.dto.HandshakeApiRequestBodyDTO;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigStore;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigStore;
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

    private final DropFileAppConfigStore appConfigStore;

    private final DropFileSecretsConfigStore secretsConfigStore;

    @Autowired
    public DaemonClient(HttpClient httpClient,
                        ObjectMapper objectMapper,
                        DropFileAppConfigStore appConfigStore,
                        DropFileSecretsConfigStore secretsConfigStore) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appConfigStore = appConfigStore;
        this.secretsConfigStore = secretsConfigStore;
    }

    @SneakyThrows
    public HttpResponse<Void> setPublicAddress(String publicAddress) {
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/config/public_address");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        byte[] payload = objectMapper.writeValueAsBytes(
                new DaemonSetPublicAddressRequestBodyDTO(publicAddress)
        );

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getDaemonInfo() {
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort());
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
    public HttpResponse<String> handshakeRequest(String nodeAddress) {
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/request");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        byte[] payload = objectMapper.writeValueAsBytes(new HandshakeApiRequestBodyDTO(nodeAddress));

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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
    public HttpResponse<byte[]> trust(String fingerprint) {
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
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

    @SneakyThrows
    public HttpResponse<String> nodePing(String fingerprint) {
        DropFileAppConfig.DropFileCliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/node/ping/")
                .resolve(fingerprint);
        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getDaemonAuthorizationToken() {
        DropFileSecretsConfig secretsConfig = secretsConfigStore.getRequired();

        String daemonToken = Objects.requireNonNull(secretsConfig.daemonToken());
        return "Bearer " + daemonToken;
//        return "fake";
    }
}
