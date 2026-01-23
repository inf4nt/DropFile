package com.evolution.dropfiledaemon.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.manifest.FileHelper;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import com.evolution.dropfiledaemon.util.Trying;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
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

    private final FileHelper fileHelper;

    @Autowired
    public FileDownloadOrchestrator(TunnelClient tunnelClient,
                                    AppConfigStore appConfigStore,
                                    FileHelper fileHelper) {
        this.tunnelClient = tunnelClient;
        this.appConfigStore = appConfigStore;
        this.fileHelper = fileHelper;
    }

    @SneakyThrows
    public FileDownloadResponse start(FileDownloadRequest request) {
        if (downloadingSemaphore.availablePermits() == 0) {
            throw new IllegalStateException("No available permits. Total: " + MAX_PARALLEL_DOWNLOADING_COUNT);
        }

        downloadingSemaphore.acquire();
        File file = Trying
                .call(() -> getFile(request))
                .doOnError(exception -> {
                    downloadingSemaphore.release();
                    log.info("Exception occurred during getting file {}", exception.getMessage(), exception);
                })
                .build()
                .getOrElseThrow();
        String operationId = UUID.randomUUID().toString();
        ExecutorService downloadProcedureExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        DownloadProcedure downloadProcedure = new DownloadProcedure(
                new Semaphore(MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT), downloadProcedureExecutorService,
                operationId, request.id(), file
        );
        downloadProcedures.put(operationId, downloadProcedure);
        // TODO create a temp file, download data to the temp file, check hash, move temp file to real file
        fileDownloadingExecutorService.execute(() ->
                Trying.call(() -> downloadProcedure.run())
                        .doOnError(exception -> {
                            log.info("Exception occurred during download process {}", exception.getMessage(), exception);
                        })
                        .doFinally(() -> {
                            downloadingSemaphore.release();
                            downloadProcedureExecutorService.shutdown();
                        })
                        .build()
                        .getOrElseThrow()
        );
        return new FileDownloadResponse(operationId, file.getAbsolutePath());
    }

    public List<DownloadProgress> getDownloadProgressList() {
        return downloadProcedures.values()
                .stream()
                .map(it -> it.getProgress())
                .toList();
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

                        MessageDigest sha256MessageDigest = CommonUtils.getMessageDigestSha256();

                        this.manifest = ExecutionProfiling.run(
                                String.format("share-download-manifest id %s", id),
                                () -> downloadManifest()
                        );

                        ExecutionProfiling.run(
                                String.format("share-download-chunks id %s %s size %s", id, file.getAbsolutePath(), manifest.chunkManifests().size()),
                                () -> downloadAndWriteChunks(sha256MessageDigest)
                        );

                        String sha256 = fileHelper.bytesToHex(sha256MessageDigest.digest());
                        if (!manifest.hash().equals(sha256)) {
                            throw new RuntimeException(String.format(
                                    "File sha256 mismatch. Actual %s expected %s file id %s", sha256, manifest.hash(), id
                            ));
                        }
                    }
            );
        }

        private void downloadAndWriteChunks(MessageDigest sha256MessageDigest) throws Exception {
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
                                if (exceptionAtomicReference.get() != null) {
                                    return;
                                }
                                Trying.call(() -> {
                                            try (InputStream inputStream = chunkDownloadStream(chunkManifest.startPosition(), chunkManifest.endPosition())) {
                                                writeChunkToFile(fileChannel, inputStream, sha256MessageDigest, chunkManifest.startPosition(), chunkManifest.size());
                                            }
                                        })
                                        .doOnError(exception -> exceptionAtomicReference.set(exception))
                                        .doFinally(() -> chunkDownloadingSemaphore.release())
                                        .build()
                                        .getOrElseThrow();
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
            return Trying.call(() ->
                            RetryExecutor.call(
                                            () -> tunnelClient.send(
                                                    TunnelClient.Request.builder()
                                                            .command("share-download-manifest")
                                                            .body(id)
                                                            .build(),
                                                    ShareDownloadManifestResponse.class
                                            )
                                    )
                                    .doOnError((attempt, exception) -> {
                                        log.info("Retry 'share-download-manifest'. Attempt: {} {}", attempt, exception.getMessage());
                                    })
                                    .doOnSuccessful((attempt, manifest) -> {
                                        log.info(
                                                "Manifest downloaded 'share-download-manifest'. Attempt: {} Hash {} Chunk size {}",
                                                attempt, manifest.hash(), manifest.chunkManifests().size()
                                        );
                                    })
                                    .build()
                                    .run()
                    )
                    .build()
                    .getOrElseThrow(exception -> new RuntimeException(String.format("Unable to download manifest %s", id)));
        }

        @SneakyThrows
        private InputStream chunkDownloadStream(long start, long end) {
            return Trying.call(() ->
                            RetryExecutor.call(
                                            () -> tunnelClient.stream(
                                                    TunnelClient.Request.builder()
                                                            .command("share-download-chunk-stream")
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
                                    .build()
                                    .run()
                    )
                    .build()
                    .getOrElseThrow(exception -> new RuntimeException(String.format(
                            "Unable to download chunk %s [%s, %s]", id, start, end
                    )));
        }

        @SneakyThrows
        private void writeChunkToFile(FileChannel writeToFileChannel,
                                      InputStream inputStreamChunk,
                                      MessageDigest sha256MessageDigest,
                                      long position,
                                      int size) {
            fileHelper.write(writeToFileChannel, inputStreamChunk, sha256MessageDigest, position, size);
        }
    }

    @SneakyThrows
    private File getFile(FileDownloadRequest request) {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }
        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile = new File(downloadDirectory, request.filename());

        if (Files.notExists(downloadFile.toPath())) {
            Path parent = downloadFile.toPath().getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(downloadFile.toPath());
        }

        if (!request.rewrite() && Files.size(downloadFile.toPath()) != 0) {
            throw new RuntimeException("Unable to rewrite file");
        }
        return downloadFile;
    }

    public record DownloadProgress(String operationId,
                                   String id,
                                   String filename,
                                   long downloaded,
                                   long total,
                                   String percentage) {

    }
}
