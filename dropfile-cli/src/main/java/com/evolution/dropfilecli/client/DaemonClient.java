package com.evolution.dropfilecli.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.DaemonSetPublicAddressRequestBodyDTO;
import com.evolution.dropfile.common.dto.HandshakeApiRequestBodyDTO;
import com.evolution.dropfile.common.dto.HandshakeIdentityRequestDTO;
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
    public HttpResponse<Void> setPublicAddress(String publicAddress) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/config/public_address");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(
                                new DaemonSetPublicAddressRequestBodyDTO(publicAddress)
                        )
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getDaemonInfo() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/shutdown");

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
    public HttpResponse<String> handshakeRequest(String publicKey, String nodeAddress) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/request");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new HandshakeApiRequestBodyDTO(publicKey, nodeAddress))
                ))
                .header("Content-Type", "application/json")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SneakyThrows
    public HttpResponse<byte[]> getIncomingRequests() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
    public HttpResponse<byte[]> getOutgoingRequest(String fingerprint) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/request/outgoing/")
                .resolve(fingerprint);

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
    public HttpResponse<byte[]> getTrustOut(String fingerprint) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/trust/out/")
                .resolve(fingerprint);

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
    public HttpResponse<byte[]> trust(String fingerprint) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

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

    @SneakyThrows
    public HttpResponse<byte[]> getIdentity(String nodeAddress) {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        URI daemonURI = CommonUtils.toURI(cliAppConfig.daemonHost(), cliAppConfig.daemonPort())
                .resolve("/api/handshake/identity");

        String daemonAuthorizationToken = getDaemonAuthorizationToken();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(daemonURI)
                .header("Authorization", daemonAuthorizationToken)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new HandshakeIdentityRequestDTO(nodeAddress))
                ))
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
