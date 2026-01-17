package com.evolution.dropfiledaemon.file;

import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelResponse;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Component
public class FileDownloadOrchestrator {

    private static final int MAX_PARALLEL_DOWNLOADING_COUNT = 10;

    private static final int MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT = 10;

    private final Semaphore downloadingSemaphore = new Semaphore(MAX_PARALLEL_DOWNLOADING_COUNT);

    private final ExecutorService fileDownloadingExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, DownloadProcedure> downloadProcedures = Collections.synchronizedMap(new LinkedHashMap<>());

    private final TunnelClient tunnelClient;

    private final AppConfigStore appConfigStore;

    @Autowired
    public FileDownloadOrchestrator(TunnelClient tunnelClient,
                                    AppConfigStore appConfigStore) {
        this.tunnelClient = tunnelClient;
        this.appConfigStore = appConfigStore;
    }

    @SneakyThrows
    public FileDownloadResponse start(FileDownloadRequest request) {
        if (downloadingSemaphore.availablePermits() == 0) {
            throw new IllegalStateException("No available permits");
        }

        downloadingSemaphore.acquire();
        File file;
        try {
            file = getFile(request);
        } catch (Exception e) {
            e.printStackTrace();
            downloadingSemaphore.release();
            throw e;
        }
        String operationId = UUID.randomUUID().toString();
        ExecutorService downloadProcedureExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        DownloadProcedure downloadProcedure = new DownloadProcedure(
                new Semaphore(MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT), downloadProcedureExecutorService,
                operationId, request.id(), file
        );
        downloadProcedures.put(operationId, downloadProcedure);
        fileDownloadingExecutorService.execute(() -> {
            try {
                downloadProcedure.run();
            } finally {
                downloadingSemaphore.release();
                downloadProcedureExecutorService.shutdown();
            }
        });
        return new FileDownloadResponse(operationId, file.getAbsolutePath());
    }

    public List<DownloadProgress> getDownloadProgressList() {
        return downloadProcedures.values()
                .stream()
                .map(it -> it.getProgress())
                .toList();
    }

    @SneakyThrows
    private File getFile(FileDownloadRequest request) {
        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile;
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new UnsupportedOperationException();
        } else {
            downloadFile = new File(new File(downloadDirectory), request.filename());
        }

        if (Files.notExists(downloadFile.toPath())) {
            Files.createFile(downloadFile.toPath());
        }

        if (!request.rewrite() && Files.size(downloadFile.toPath()) != 0) {
            throw new RuntimeException("Unable to rewrite file");
        }
        return downloadFile;
    }

    public class DownloadProcedure {

        private final LongAdder chunksHaveDownloaded = new LongAdder();

        private final Semaphore chunkDownloadingSemaphore;

        private final ExecutorService executorService;

        private final String operationId;

        private final String id;

        private final File file;

        private ShareDownloadManifestResponse manifest;

        public DownloadProcedure(Semaphore chunkDownloadingSemaphore,
                                 ExecutorService executorService,
                                 String operationId,
                                 String id,
                                 File file) {
            this.chunkDownloadingSemaphore = chunkDownloadingSemaphore;
            this.executorService = executorService;
            this.operationId = operationId;
            this.id = id;
            this.file = file;
        }

        @SneakyThrows
        public void run() {
            long start = System.currentTimeMillis();

            this.manifest = downloadManifest();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            try (FileChannel fileChannel = FileChannel.open(
                    file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE)) {
                for (ShareDownloadManifestResponse.ChunkManifest chunkManifest : manifest.chunkManifests()) {
                    chunkDownloadingSemaphore.acquire();
                    CompletableFuture<Void> future = CompletableFuture.runAsync(
                            () -> {
                                try {
                                    ShareDownloadChunkTunnelResponse chunkTunnelResponse = chunkDownload(
                                            chunkManifest.startPosition(), chunkManifest.endPosition()
                                    );
                                    writeChunkToFile(fileChannel, chunkTunnelResponse.data(), chunkManifest.startPosition());
                                } finally {
                                    chunkDownloadingSemaphore.release();
                                }
                            },
                            executorService
                    );
                    futures.add(future);
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            long end = System.currentTimeMillis();
            log.info("Downloading competed: {} {} seconds {}",
                    id,
                    file.getAbsolutePath(),
                    TimeUnit.MILLISECONDS.toSeconds(end - start)
            );
        }

        public DownloadProgress getProgress() {
            if (manifest == null) {
                return new DownloadProgress(
                        operationId,
                        id,
                        file.getAbsolutePath(),
                        0,
                        0,
                        "0.00 %"
                );
            }
            long downloaded = chunksHaveDownloaded.sum();
            String percent = percent(downloaded, manifest.size());
            return new DownloadProgress(
                    operationId,
                    id,
                    file.getAbsolutePath(),
                    downloaded,
                    manifest.size(),
                    percent
            );
        }

        private String percent(long downloaded, long total) {
            if (total <= 0) {
                return "0.0 %";
            }

            double value = (double) downloaded * 100.0 / total;
            return String.format(Locale.US, "%.2f %%", value);
        }

        private ShareDownloadManifestResponse downloadManifest() {
            long start = System.currentTimeMillis();
            log.info("Downloading manifest: {} {}", id, file.getAbsolutePath());

            ShareDownloadManifestResponse manifest = tunnelClient.send(
                    TunnelClient.Request.builder()
                            .action("share-download-manifest")
                            .body(id)
                            .build(),
                    ShareDownloadManifestResponse.class
            );

            if (manifest == null) {
                throw new RuntimeException("Manifest is null: " + id);
            }

            long end = System.currentTimeMillis();
            log.info("Downloaded manifest: {} {} chunks {} seconds {}",
                    id,
                    file.getAbsolutePath(),
                    manifest.chunkManifests().size(),
                    TimeUnit.MILLISECONDS.toSeconds(end - start)
            );
            return manifest;
        }

        @SneakyThrows
        private ShareDownloadChunkTunnelResponse chunkDownload(long start, long end) {
//        log.info("Downloading chunk: {}", chunkManifest.hash());
            ShareDownloadChunkTunnelResponse chunkResponse = tunnelClient.send(
                    TunnelClient.Request.builder()
                            .action("share-download-chunk")
                            .body(new ShareDownloadChunkTunnelRequest(
                                    id,
                                    start,
                                    end
                            ))
                            .build(),
                    ShareDownloadChunkTunnelResponse.class
            );
            if (chunkResponse == null) {
                throw new RuntimeException(
                        String.format("Chunk response is null: id %s start %s end %s", id, start, end)
                );
            }
            return chunkResponse;
        }

        @SneakyThrows
        private void writeChunkToFile(FileChannel fileChannel,
                                      byte[] data,
                                      long position) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            fileChannel.write(buffer, position);
            buffer.clear();
            chunksHaveDownloaded.add(data.length);
        }
    }

    public record DownloadProgress(String operationId,
                                   String id,
                                   String filename,
                                   long downloaded,
                                   long total,
                                   String percentage) {

    }
}
