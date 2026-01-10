package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfile.common.dto.FileEntryTunnelResponse;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ApiConnectionsShareFacade {

    private final TunnelClient tunnelClient;

    private final AppConfigStore appConfigStore;

    @Autowired
    public ApiConnectionsShareFacade(TunnelClient tunnelClient,
                                     AppConfigStore appConfigStore) {
        this.tunnelClient = tunnelClient;
        this.appConfigStore = appConfigStore;
    }

    public List<ApiConnectionsShareLsResponseDTO> ls() {
        List<FileEntryTunnelResponse> files = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-ls")
                        .build(),
                new TypeReference<List<FileEntryTunnelResponse>>() {
                }
        );
        if (files == null) {
            return Collections.emptyList();
        }
        return files.stream()
                .map(it -> new ApiConnectionsShareLsResponseDTO(it.id(), it.alias()))
                .toList();
    }

    public String cat(String id) {
        return tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-cat")
                        .body(id)
                        .build(),
                String.class
        );
    }

    @SneakyThrows
    public ApiConnectionsShareDownloadResponseDTO download(ApiConnectionsShareDownloadRequestDTO requestDTO) {
        long startManifest = System.currentTimeMillis();
        log.info("Manifest downloading {}", requestDTO.id());
        ShareDownloadManifestResponse manifestResponse = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-download-manifest")
                        .body(requestDTO.id())
                        .build(),
                ShareDownloadManifestResponse.class
        );
        long endManifest = System.currentTimeMillis();
        log.info("Manifest downloaded {} seconds {}", requestDTO.id(), TimeUnit.MILLISECONDS.toSeconds(endManifest - startManifest));
        if (manifestResponse == null) {
            throw new RuntimeException("Unable to download manifest: " + requestDTO.id());
        }

        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile;
        if (ObjectUtils.isEmpty(requestDTO.filename())) {
            throw new UnsupportedOperationException();
        } else {
            downloadFile = new File(new File(downloadDirectory), requestDTO.filename());
        }

        if (Files.notExists(downloadFile.toPath())) {
            Files.createFile(downloadFile.toPath());
        }

        if (!requestDTO.rewrite() && Files.size(downloadFile.toPath()) != 0) {
            throw new RuntimeException("Unable to rewrite file");
        }

        long start = System.currentTimeMillis();

        log.info("Executing file {} downloading", manifestResponse.id());

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<Future> futures = new ArrayList<>();

        try (FileChannel channel = FileChannel.open(
                downloadFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {

            for (ShareDownloadManifestResponse.ChunkManifest chunkManifest : manifestResponse.chunkManifests()) {
                Future<?> submit = executorService.submit(new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        log.info("Chunk {}", chunkManifest.hash());
                        ShareDownloadChunkTunnelResponse chunkResponse = tunnelClient.send(
                                TunnelClient.Request.builder()
                                        .action("share-download-chunk")
                                        .body(new ShareDownloadChunkTunnelRequest(
                                                manifestResponse.id(),
                                                chunkManifest.startPosition(),
                                                chunkManifest.endPosition()
                                        ))
                                        .build(),
                                ShareDownloadChunkTunnelResponse.class
                        );
                        ByteBuffer buffer =
                                ByteBuffer.wrap(chunkResponse.data());

                        channel.write(buffer, chunkManifest.startPosition());
                    }
                });
                futures.add(submit);
            }

            for (Future future : futures) {
                while (!future.isDone()) {

                }
            }

            executorService.shutdown();
        }

        long end = System.currentTimeMillis();

        log.info("Finished downloading {} seconds {}", manifestResponse.id(), TimeUnit.MILLISECONDS.toSeconds(end - start));

        return new ApiConnectionsShareDownloadResponseDTO(downloadFile.getAbsolutePath());
    }
}
