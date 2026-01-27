package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfiledaemon.download.exception.ChecksumMismatchException;
import com.evolution.dropfiledaemon.download.exception.ChunkDownloadingFailedException;
import com.evolution.dropfiledaemon.download.exception.DownloadingStoppedException;
import com.evolution.dropfiledaemon.download.exception.ManifestDownloadingFailedException;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestResponse;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.FileHelper;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import com.evolution.dropfiledaemon.util.SafeUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadOrchestrator {

    private static final int MAX_PARALLEL_DOWNLOADING_COUNT = 10;

    private static final int MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT = 1;

    private final Semaphore downloadingSemaphore = new Semaphore(MAX_PARALLEL_DOWNLOADING_COUNT);

    private final ExecutorService fileDownloadingExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, DownloadProcedure> downloadProcedures = Collections.synchronizedMap(new LinkedHashMap<>());

    private final TunnelClient tunnelClient;

    private final AppConfigStore appConfigStore;

    private final FileHelper fileHelper;

    private final FileDownloadEntryStore fileDownloadEntryStore;

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
            SafeUtil.execute(() -> downloadingSemaphore.release());
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
        fileDownloadEntryStore.save(
                operationId,
                new DownloadFileEntry(
                        request.fileId(),
                        destinationFile.getAbsolutePath(),
                        temporaryFile.getAbsolutePath(),
                        null,
                        0,
                        0,
                        DownloadFileEntry.DownloadFileEntryStatus.DOWNLOADING,
                        Instant.now()
                )
        );
        fileDownloadingExecutorService.execute(() -> {
            try {
                downloadProcedure.run();
                fileDownloadEntryStore.save(
                        operationId,
                        new DownloadFileEntry(
                                request.fileId(),
                                destinationFile.getAbsolutePath(),
                                temporaryFile.getAbsolutePath(),
                                downloadProcedure.getProgress().hash(),
                                downloadProcedure.getProgress().downloaded(),
                                downloadProcedure.getProgress().total(),
                                DownloadFileEntry.DownloadFileEntryStatus.COMPLETED,
                                Instant.now()
                        )
                );
            } catch (Exception exception) {
                DownloadFileEntry.DownloadFileEntryStatus status = exception instanceof DownloadingStoppedException ?
                        DownloadFileEntry.DownloadFileEntryStatus.STOPPED :
                        DownloadFileEntry.DownloadFileEntryStatus.ERROR;

                fileDownloadEntryStore.save(
                        operationId,
                        new DownloadFileEntry(
                                request.fileId(),
                                destinationFile.getAbsolutePath(),
                                temporaryFile.getAbsolutePath(),
                                downloadProcedure.getProgress().hash(),
                                downloadProcedure.getProgress().downloaded(),
                                downloadProcedure.getProgress().total(),
                                status,
                                Instant.now()
                        )
                );
                log.info("Exception occurred during download process {}", exception.getMessage(), exception);
                throw new RuntimeException(exception);
            } finally {
                SafeUtil.execute(() -> downloadProcedures.remove(operationId));
                SafeUtil.execute(() -> downloadProcedureExecutorService.close());
                SafeUtil.execute(() -> downloadingSemaphore.release());
            }
        });
        return new FileDownloadResponse(operationId, destinationFile.getAbsolutePath());
    }

    public Map<String, DownloadProgress> getDownloadProcedures() {
        Map<String, DownloadProgress> result = new LinkedHashMap<>();
        for (Map.Entry<String, DownloadProcedure> entry : downloadProcedures.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getProgress());
        }
        return result;
    }

    public void stopAll() {
        for (DownloadProcedure procedure : downloadProcedures.values()) {
            procedure.stop();
        }
    }

    public boolean stop(String operation) {
        DownloadProcedure downloadProcedure = downloadProcedures.get(operation);
        if (downloadProcedure != null) {
            downloadProcedure.stop();
            return true;
        }
        return false;
    }

    public class DownloadProcedure {

        private final AtomicBoolean stop = new AtomicBoolean(false);

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
                        String.format("file-download-orchestrator operation: %s fileId: %s", operationId, request.fileId()),
                        () -> {

                            ExecutionProfiling.run(
                                    String.format("download-manifest operation: %s fileId: %s", operationId, request.fileId()),
                                    () -> downloadManifest()
                            );

                            ExecutionProfiling.run(
                                    String.format("download-chunks operation: %s fileId: %s: chunks %s",
                                            operationId, request.fileId(), manifest.chunkManifests().size()
                                    ),
                                    () -> downloadAndWriteChunks()
                            );

                            String actualSha256 = ExecutionProfiling.run(
                                    String.format("digest-calculation operation: %s fileId: %s", operationId, request.fileId()),
                                    () -> fileHelper.sha256(temporaryFile)
                            );

                            if (!manifest.hash().equals(actualSha256)) {
                                throw new ChecksumMismatchException(operationId, manifest.hash(), actualSha256);
                            }

                            checkIfProcessHasStopped();

                            Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                        }
                );
            } finally {
                if (temporaryFile != null && Files.exists(temporaryFile.toPath())) {
                    SafeUtil.execute(() -> Files.delete(temporaryFile.toPath()));
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
                                try (InputStream inputStream = chunkDownloadStream(chunkManifest)) {
                                    writeChunkToFile(fileChannel, inputStream, chunkManifest);
                                } catch (Exception exception) {
                                    exceptionAtomicReference.set(exception);
                                } finally {
                                    SafeUtil.execute(() -> chunkDownloadingSemaphore.release());
                                }
                            },
                            executorService
                    );
                    futures.add(future);
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        }

        public void stop() {
            stop.set(true);
        }

        public DownloadProgress getProgress() {
            if (manifest == null) {
                return new DownloadProgress(
                        operationId,
                        request.fileId(),
                        destinationFile.getAbsolutePath(),
                        null,
                        0,
                        0,
                        "0.00 %"
                );
            }
            long downloaded = chunksHaveDownloaded.sum();
            String percent = fileHelper.percent(downloaded, manifest.size());
            return new DownloadProgress(
                    operationId,
                    request.fileId(),
                    destinationFile.getAbsolutePath(),
                    manifest.hash(),
                    downloaded,
                    manifest.size(),
                    percent
            );
        }

        private void downloadManifest() {
            try {
                this.manifest = RetryExecutor.call(
                                () -> {
                                    checkIfProcessHasStopped();
                                    return tunnelClient.send(
                                            TunnelClient.Request.builder()
                                                    .command("share-download-manifest")
                                                    .body(request.fileId())
                                                    .build(),
                                            ShareDownloadManifestResponse.class);
                                }
                        )
                        .doOnError((attempt, exception) -> {
                            log.info("Retry 'share-download-manifest'. Operation: {} fileId: {} filename: {} attempt: {} exception: {}",
                                    operationId, request.fileId(), request.filename(), attempt, exception.getMessage()
                            );
                        })
                        .doOnSuccessful((attempt, manifest) -> {
                            log.info(
                                    "Manifest downloaded 'share-download-manifest'. Operation: {} fileId: {} filename: {} Attempt: {} Hash {} Chunk size {}",
                                    operationId, request.fileId(), request.filename(), attempt, manifest.hash(), manifest.chunkManifests().size()
                            );
                        })
                        .retryIf(it -> {
                            if (it.exception() instanceof DownloadingStoppedException) {
                                return false;
                            }
                            return it.exception() != null || it.result() == null;
                        })
                        .build()
                        .run();
            } catch (Exception e) {
                throw new ManifestDownloadingFailedException(operationId, request.fileId(), request.filename(), e);
            }
        }

        private InputStream chunkDownloadStream(ShareDownloadManifestResponse.ChunkManifest chunkManifest) {
            try {
                return RetryExecutor.call(
                                () -> {
                                    checkIfProcessHasStopped();
                                    return tunnelClient.stream(
                                            TunnelClient.Request.builder()
                                                    .command("share-download-chunk-stream")
                                                    .body(new ShareDownloadChunkTunnelRequest(
                                                            request.fileId(),
                                                            chunkManifest.startPosition(),
                                                            chunkManifest.endPosition()
                                                    ))
                                                    .build());
                                }
                        )
                        .doOnError((attempt, exception) -> {
                            log.info("Retry 'share-download-chunk-stream'. Attempt: {} start {} end {} exception {}",
                                    attempt, chunkManifest.startPosition(), chunkManifest.endPosition(), exception.getMessage());
                        })
                        .retryIf(it -> {
                            if (it.exception() instanceof DownloadingStoppedException) {
                                return false;
                            }
                            //noinspection resource
                            return it.exception() != null || it.result() == null;
                        })
                        .build()
                        .run();
            } catch (Exception e) {
                throw new ChunkDownloadingFailedException(operationId, chunkManifest.hash(), chunkManifest.startPosition(), chunkManifest.endPosition(), e);
            }
        }

        @SneakyThrows
        private void writeChunkToFile(FileChannel writeToFileChannel,
                                      InputStream inputStreamChunk,
                                      ShareDownloadManifestResponse.ChunkManifest chunkManifest) {
            checkIfProcessHasStopped();
            fileHelper.write(writeToFileChannel, inputStreamChunk, chunkManifest.startPosition());
            chunksHaveDownloaded.add(chunkManifest.size());
        }

        public void checkIfProcessHasStopped() {
            if (stop.get()) {
                throw new DownloadingStoppedException(operationId);
            }
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
                                   String fileId,
                                   String filename,
                                   String hash,
                                   long downloaded,
                                   long total,
                                   String percentage) {

    }
}
