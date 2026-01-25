package com.evolution.dropfiledaemon.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.manifest.FileHelper;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        File destinationFile;
        File temporaryFile;
        try {
            destinationFile = getDestinationFile(request);
            temporaryFile = getTemporaryFile(request);
        } catch (Exception exception) {
            downloadingSemaphore.release();
            log.info("Exception occurred during getting file {}", exception.getMessage(), exception);
            throw exception;
        }

        String operationId = CommonUtils.random();
        ExecutorService downloadProcedureExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        DownloadProcedure downloadProcedure = new DownloadProcedure(
                new Semaphore(MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT),
                downloadProcedureExecutorService,
                operationId,
                request,
                destinationFile,
                temporaryFile
        );
        downloadProcedures.put(operationId, downloadProcedure);
        fileDownloadingExecutorService.execute(() -> {
            try {
                downloadProcedure.run();
            } catch (Exception exception) {
                log.info("Exception occurred during download process {}", exception.getMessage(), exception);
                throw new RuntimeException(exception);
            } finally {
                downloadingSemaphore.release();
                downloadProcedureExecutorService.shutdown();
            }
        });
        return new FileDownloadResponse(operationId, destinationFile.getAbsolutePath());
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

        private final FileDownloadRequest request;

        private final File destinationFile;

        private final File temporaryFile;

        private ShareDownloadManifestResponse manifest;

        public DownloadProcedure(Semaphore chunkDownloadingSemaphore,
                                 ExecutorService executorService,
                                 String operationId,
                                 FileDownloadRequest request,
                                 File destinationFile,
                                 File temporaryFile) {
            this.chunkDownloadingSemaphore = chunkDownloadingSemaphore;
            this.executorService = executorService;
            this.operationId = operationId;
            this.request = request;
            this.destinationFile = destinationFile;
            this.temporaryFile = temporaryFile;
        }

        @SneakyThrows
        public void run() {
            try {
                ExecutionProfiling.run(
                        String.format("file-download-orchestrator operation %s file id %s", operationId, request.id()),
                        () -> {

                            ExecutionProfiling.run(
                                    String.format("download-manifest operation %s file id %s", operationId, request.id()),
                                    () -> downloadManifest()
                            );

                            ExecutionProfiling.run(
                                    String.format("download-chunks operation %s file id %s chunks %s",
                                            operationId, request.id(), manifest.chunkManifests().size()
                                    ),
                                    () -> downloadAndWriteChunks()
                            );

                            String actualSha256 = ExecutionProfiling.run(
                                    String.format("digest-calculation operation %s file id %s", operationId, request.id()),
                                    () -> fileHelper.sha256(temporaryFile)
                            );

                            log.info("Actual sha256. operation {} sha256 {} file id {}", operationId, actualSha256, request.id());

                            if (!manifest.hash().equals(actualSha256)) {
                                throw new RuntimeException(String.format(
                                        "Mismatch sha256. Actual %s expected %s file id %s", actualSha256, manifest.hash(), request.id()
                                ));
                            }

                            Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                        }
                );
            } finally {
                if (temporaryFile != null && Files.exists(temporaryFile.toPath())) {
                    Files.delete(temporaryFile.toPath());
                }
            }
        }

        private void downloadAndWriteChunks() throws Exception {
            AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            try (FileChannel fileChannel = FileChannel.open(
                    temporaryFile.toPath(),
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
                                try (InputStream inputStream = chunkDownloadStream(chunkManifest.startPosition(), chunkManifest.endPosition())) {
                                    writeChunkToFile(fileChannel, inputStream, chunkManifest.startPosition(), chunkManifest.size());
                                } catch (Exception exception) {
                                    exceptionAtomicReference.set(exception);
                                    throw new RuntimeException(exception);
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
        }

        public DownloadProgress getProgress() {
            if (manifest == null) {
                return new DownloadProgress(
                        operationId,
                        request.id(),
                        destinationFile.getAbsolutePath(),
                        0,
                        0,
                        "0.00 %"
                );
            }
            long downloaded = chunksHaveDownloaded.sum();
            String percent = percent(downloaded, manifest.size());
            return new DownloadProgress(
                    operationId,
                    request.id(),
                    destinationFile.getAbsolutePath(),
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

        private void downloadManifest() {
            try {
                this.manifest = RetryExecutor.call(
                                () -> tunnelClient.send(
                                        TunnelClient.Request.builder()
                                                .command("share-download-manifest")
                                                .body(request.id())
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
                        .run();
            } catch (Exception e) {
                throw new RuntimeException(String.format("Unable to download manifest %s", request.id()), e);
            }
        }

        private InputStream chunkDownloadStream(long start, long end) {
            try {
                return RetryExecutor.call(
                                () -> tunnelClient.stream(
                                        TunnelClient.Request.builder()
                                                .command("share-download-chunk-stream")
                                                .body(new ShareDownloadChunkTunnelRequest(
                                                        request.id(),
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
                        .run();
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Unable to download chunk %s [%s, %s]", request.id(), start, end
                ), e);
            }
        }

        @SneakyThrows
        private void writeChunkToFile(FileChannel writeToFileChannel,
                                      InputStream inputStreamChunk,
                                      long position,
                                      int size) {
            fileHelper.write(writeToFileChannel, inputStreamChunk, position);
            chunksHaveDownloaded.add(size);
        }
    }

    private File getDestinationFile(FileDownloadRequest request) throws IOException {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }

        if (Paths.get(request.filename()).isAbsolute()) {
            throw new UnsupportedOperationException("Absolute paths are not supported yet: " + request.filename());
        }

        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile = new File(downloadDirectory, request.filename()).getCanonicalFile();

        if (Files.exists(downloadFile.toPath())) {
            throw new IllegalArgumentException(String.format("file already exists: %s", downloadFile.getAbsolutePath()));
        }

        return downloadFile;
    }

    private File getTemporaryFile(FileDownloadRequest request) throws IOException {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }
        if (Paths.get(request.filename()).isAbsolute()) {
            throw new UnsupportedOperationException("filename must not be absolute. Unsupported yet: " + request.filename());
        }

        String filenamePrefix = CommonUtils.random();
        String filename = String.format("Unconfirmed-%s-%s.crdownload", filenamePrefix, request.filename());
        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile = new File(downloadDirectory, filename).getCanonicalFile();

        if (Files.notExists(downloadFile.toPath())) {
            Files.createFile(downloadFile.toPath());
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
