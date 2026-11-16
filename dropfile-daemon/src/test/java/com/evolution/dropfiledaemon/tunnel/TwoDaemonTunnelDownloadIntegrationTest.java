package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfile.common.dto.ApiHandshakeRequestDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.facade.ApiConnectionsShareFacade;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.service.AccessKeyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TwoDaemonTunnelDownloadIntegrationTest {

    private static final int DOWNLOAD_COUNT = 20;

    private static final Duration ACCESS_KEY_TTL = Duration.ofSeconds(30);

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    private Path tempDirectory;

    private ConfigurableApplicationContext daemonA;

    private ConfigurableApplicationContext daemonB;

    @BeforeEach
    void before() {
        daemonA = startDaemon("daemon-a");
        daemonB = startDaemon("daemon-b");
    }

    @AfterEach
    void after() {
        if (daemonB != null) {
            daemonB.close();
        }
        if (daemonA != null) {
            daemonA.close();
        }
    }

    @Test
    void downloadsSharedNumbersFileThroughBrowseGetEndpointAndTunnel() throws Exception {
        String daemonBAddress = getBaseAddress(daemonB);
        createAccessKeyAndHandshake(daemonA, daemonB, daemonBAddress);

        Path sourceFile = Path.of(getClass().getClassLoader().getResource("numbers.txt").toURI());
        byte[] expectedContents = Files.readAllBytes(sourceFile);
        ApiConnectionsShareLsResponseDTO sharedFile = daemonB.getBean(ApiConnectionsShareFacade.class)
                .add(new ApiConnectionsShareAddRequestDTO(sourceFile.toString(), "numbers.txt"), null);
        assertThat(sharedFile.accessible(), is(true));

        ObjectMapper objectMapper = daemonA.getBean(ObjectMapper.class);
        DaemonSecret daemonSecret = daemonA.getBean(DaemonSecretStore.class)
                .get()
                .orElseThrow();
        HttpClient httpClient = HttpClient.newHttpClient();
        String authorization = "Bearer " + daemonSecret.daemonToken();

        HttpResponse<byte[]> browseResponse = post(
                httpClient,
                objectMapper,
                daemonA,
                "/api/connections/browse/ls",
                authorization,
                new ApiConnectionsBrowseLsRequestDTO(List.of())
        );
        assertThat(browseResponse.statusCode(), is(200));
        List<ApiConnectionsBrowseLsResponseDTO> remoteFiles = objectMapper.readValue(
                browseResponse.body(),
                new TypeReference<>() {
                }
        );
        ApiConnectionsBrowseLsResponseDTO remoteFile = remoteFiles.stream()
                .filter(file -> file.id().equals(sharedFile.id()))
                .findFirst()
                .orElseThrow();

        FileDownloadStore downloadStore = daemonA.getBean(FileDownloadStore.class);
        List<CompletableFuture<HttpResponse<byte[]>>> downloadResponses = new ArrayList<>(DOWNLOAD_COUNT);
        for (int i = 1; i <= DOWNLOAD_COUNT; i++) {
            downloadResponses.add(postAsync(
                    httpClient,
                    objectMapper,
                    daemonA,
                    "/api/connections/browse/get",
                    authorization,
                    new ApiConnectionsBrowseGetRequestDTO(
                            new CriteriaEnvelope(remoteFile.id()),
                            "file_" + i
                    )
            ));
        }

        CompletableFuture.allOf(downloadResponses.toArray(CompletableFuture[]::new)).join();

        List<ApiConnectionsBrowseGetResponseDTO> downloads = new ArrayList<>(DOWNLOAD_COUNT);
        for (CompletableFuture<HttpResponse<byte[]>> downloadResponseFuture : downloadResponses) {
            HttpResponse<byte[]> downloadResponse = downloadResponseFuture.join();
            assertThat(downloadResponse.statusCode(), is(200));
            ApiConnectionsBrowseGetResponseDTO download = objectMapper.readValue(
                    downloadResponse.body(),
                    ApiConnectionsBrowseGetResponseDTO.class
            );
            assertThat(download.fingerprint(), is(remoteFile.fingerprint()));
            assertThat(download.fileId(), is(remoteFile.id()));
            downloads.add(download);
        }

        assertThat(downloads, hasSize(DOWNLOAD_COUNT));
        assertThat(downloads.stream().map(ApiConnectionsBrowseGetResponseDTO::filename).count(),
                is((long) DOWNLOAD_COUNT));
        assertThat(downloads.stream().map(ApiConnectionsBrowseGetResponseDTO::operationId).count(),
                is((long) DOWNLOAD_COUNT));

        for (ApiConnectionsBrowseGetResponseDTO download : downloads) {
            DownloadFile completedDownload = awaitDownload(downloadStore, download.operationId());

            assertThat(completedDownload.status(), is(DownloadFile.DownloadFileStatus.COMPLETED));
            assertThat(completedDownload.fileId(), is(sharedFile.id()));
            assertThat(completedDownload.destinationFile(), is(download.filename()));
            assertThat(
                    Arrays.equals(
                            Files.readAllBytes(Path.of(completedDownload.destinationFile())),
                            expectedContents
                    ),
                    is(true)
            );
        }
    }

    private void createAccessKeyAndHandshake(ConfigurableApplicationContext daemonA,
                                             ConfigurableApplicationContext daemonB,
                                             String daemonBAddress) {
        AccessKeyService accessKeyService = daemonB.getBean(AccessKeyService.class);
        AccessKeyStore accessKeyStore = daemonB.getBean(AccessKeyStore.class);
        AccessKeyService.KeyEnvelope accessKey = accessKeyService.generate();
        accessKeyStore.save(
                accessKey.id(),
                new AccessKey(accessKey.key(), ACCESS_KEY_TTL, Instant.now())
        );

        daemonA.getBean(ApiHandshakeFacade.class).handshake(
                new ApiHandshakeRequestDTO(daemonBAddress, accessKey.key())
        );
        assertThat(accessKeyStore.get(accessKey.id()).isEmpty(), is(true));
    }

    private DownloadFile awaitDownload(FileDownloadStore downloadStore, String operationId) throws Exception {
        long deadline = System.nanoTime() + DOWNLOAD_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                DownloadFile download = downloadStore.get(operationId)
                        .map(java.util.Map.Entry::getValue)
                        .orElse(null);
                if (download.status() == DownloadFile.DownloadFileStatus.COMPLETED) {
                    return download;
                }
            } catch (Exception _) {
                Thread.sleep(50);
            }
            Thread.sleep(50);
        }

        throw new AssertionError("Download did not complete within " + DOWNLOAD_TIMEOUT);
    }

    private HttpResponse<byte[]> post(HttpClient httpClient,
                                      ObjectMapper objectMapper,
                                      ConfigurableApplicationContext context,
                                      String path,
                                      String authorization,
                                      Object requestBody) throws Exception {
        return httpClient.send(
                createPostRequest(objectMapper, context, path, authorization, requestBody),
                HttpResponse.BodyHandlers.ofByteArray()
        );
    }

    private CompletableFuture<HttpResponse<byte[]>> postAsync(HttpClient httpClient,
                                                              ObjectMapper objectMapper,
                                                              ConfigurableApplicationContext context,
                                                              String path,
                                                              String authorization,
                                                              Object requestBody) throws Exception {
        return httpClient.sendAsync(
                createPostRequest(objectMapper, context, path, authorization, requestBody),
                HttpResponse.BodyHandlers.ofByteArray()
        );
    }

    private HttpRequest createPostRequest(ObjectMapper objectMapper,
                                          ConfigurableApplicationContext context,
                                          String path,
                                          String authorization,
                                          Object requestBody) throws Exception {
        return HttpRequest.newBuilder()
                .uri(URI.create(getBaseAddress(context) + path))
                .header("Content-Type", "application/json")
                .header("Authorization", authorization)
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(requestBody)))
                .build();
    }

    private ConfigurableApplicationContext startDaemon(String name) {
        Path home = tempDirectory.resolve(name);
        return new SpringApplicationBuilder(DropFileDaemonApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--spring.lifecycle.timeout-per-shutdown-phase=0",
                        "--dropfile.daemon.application-home.directory=" + home,
                        "--dropfile.daemon.security.replay.max-future-drift-timeout=0",
                        "--dropfile.daemon.download.orchestrator.active-queue-size=" + DOWNLOAD_COUNT,
                        "--dropfile.daemon.server.servlet.rate-request.tunnel.limit.max=" + DOWNLOAD_COUNT
                );
    }

    private String getBaseAddress(ConfigurableApplicationContext context) {
        WebServerApplicationContext webContext = (WebServerApplicationContext) context;
        return "http://127.0.0.1:%d".formatted(webContext.getWebServer().getPort());
    }
}
