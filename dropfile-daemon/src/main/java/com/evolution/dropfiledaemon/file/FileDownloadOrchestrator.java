package com.evolution.dropfiledaemon.file;

import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import com.evolution.dropfiledaemon.utils.ExecutionProfiling;
import com.evolution.dropfiledaemon.utils.RetryExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Component
public class FileDownloadOrchestrator {

    private static final int MAX_PARALLEL_DOWNLOADING_COUNT = 10;

    private static final int MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT = 1;

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
            downloadingSemaphore.release();
            log.info("Exception occurred during getting file {}", e.getMessage(), e);
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
            } catch (Exception e) {
                log.info("Exception occurred during download process {}", e.getMessage(), e);
                throw e;
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
            ExecutionProfiling.run(
                    String.format("file-download-orchestrator %s %s", id, file.getAbsolutePath()),
                    () -> {
                        this.manifest = ExecutionProfiling.run(
                                String.format("share-download-manifest id %s", id),
                                () -> downloadManifest()
                        );

                        ExecutionProfiling.run(
                                String.format("share-download-chunks id %s %s size %s", id, file.getAbsolutePath(), manifest.chunkManifests().size()),
                                () -> downloadAndWriteChunks()
                        );
                    }
            );
        }

        private void downloadAndWriteChunks() throws Exception {
            AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            try (FileChannel fileChannel = FileChannel.open(
                    file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE)) {
                for (ShareDownloadManifestResponse.ChunkManifest chunkManifest : manifest.chunkManifests()) {
                    chunkDownloadingSemaphore.acquire();
                    if (exceptionAtomicReference.get() != null) {
                        throw exceptionAtomicReference.get();
                    }
                    CompletableFuture<Void> future = CompletableFuture.runAsync(
                            () -> {
                                InputStream inputStream = null;
                                try {
                                    inputStream = chunkDownloadStream(chunkManifest.startPosition(), chunkManifest.endPosition());
                                    writeChunkToFile(fileChannel, inputStream, chunkManifest.startPosition(), chunkManifest.size());
                                } catch (Exception e) {
                                    exceptionAtomicReference.set(e);
                                } finally {
                                    chunkDownloadingSemaphore.release();
                                    try {
                                        if (inputStream != null) {
                                            inputStream.close();
                                        }
                                    } catch (Exception e) {
                                        exceptionAtomicReference.set(e);
                                    }
                                }
                            },
                            executorService
                    );

                    futures.add(future);
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
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

        @SneakyThrows
        private ShareDownloadManifestResponse downloadManifest() {
            return RetryExecutor
                    .call(
                            () -> tunnelClient.send(
                                    TunnelClient.Request.builder()
                                            .action("share-download-manifest")
                                            .body(id)
                                            .build(),
                                    ShareDownloadManifestResponse.class
                            )
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'share-download-manifest'. Attempt: {} {}", attempt, exception.getMessage());
                    })
                    .exceptionMapper(exceptions -> new RuntimeException(String.format(
                            "Unable to download manifest %s", id
                    )))
                    .build()
                    .run();
        }

        @SneakyThrows
        private InputStream chunkDownloadStream(long start, long end) {
            return RetryExecutor
                    .call(
                            () -> tunnelClient.stream(
                                    TunnelClient.Request.builder()
                                            .action("share-download-chunk-stream")
                                            .body(new ShareDownloadChunkTunnelRequest(
                                                    id,
                                                    start,
                                                    end
                                            ))
                                            .build()
                            )
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'share-download-chunk-stream'. Attempt: {} {}", attempt, exception.getMessage());
                    })
                    .exceptionMapper(exceptions -> new RuntimeException(String.format(
                            "Unable to download chunk %s [%s, %s]", id, start, end
                    )))
                    .build()
                    .run();
        }

        @SneakyThrows
        private void writeChunkToFile(FileChannel fileChannel,
                                      InputStream inputStream,
                                      long position,
                                      long size) {
            ReadableByteChannel rbc = Channels.newChannel(inputStream);
            fileChannel.transferFrom(rbc, position, size);
//            chunksHaveDownloaded.add(data.length);
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
