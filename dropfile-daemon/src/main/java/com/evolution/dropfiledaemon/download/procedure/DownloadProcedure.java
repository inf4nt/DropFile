package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.ProcedureExceptions;
import com.evolution.dropfiledaemon.manifest.ChunkManifest;
import com.evolution.dropfiledaemon.manifest.FileManifest;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestCommandRequest;
import com.evolution.dropfiledaemon.util.DownloadSpeedMeter;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class DownloadProcedure {

    private final DownloadSpeedMeter downloadSpeedMeter = new DownloadSpeedMeter();

    private final ObjectMapper objectMapper;

    private final TunnelClient tunnelClient;

    private final FileHelper fileHelper;

    private final FileManifestBuilder fileManifestBuilder;

    private final DownloadProcedureConfiguration configuration;

    @Getter
    private final DownloadProcedureRequest request;

    private ExecutorService executorService;

    private FileManifest manifest;

    @Getter
    volatile private boolean stopped;

    public void stop() {
        if (stopped) {
            return;
        }
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }
    }

    private ExecutorService getExecutorService() {
        if (stopped) {
            throw new IllegalStateException("Download procedure already stopped: " + request.operation());
        }
        if (executorService == null) {
            synchronized (this) {
                if (stopped) {
                    throw new IllegalStateException("Download procedure already stopped: " + request.operation());
                }
                if (executorService == null) {
                    executorService = Executors.newVirtualThreadPerTaskExecutor();
                }
            }
        }
        return executorService;
    }

    public void run(Runnable beforeProcedureCallback,
                    Runnable successCallback) {
        try (ExecutorService executorService = getExecutorService()) {
            CompletableFuture.runAsync(
                            () -> {
                                beforeProcedureCallback.run();
                                runProcedure();
                                successCallback.run();
                            },
                            executorService
                    )
                    .join();
        }
    }

    private void runProcedure() {
        ExecutionProfiling.run(
                String.format("file-download-prodecure operation: %s fingerprint %s fileId: %s",
                        request.operation(), request.fingerprint(), request.fileId()),
                () -> {

                    ExecutionProfiling.run(
                            String.format("download-manifest operation: %s fingerprint %s fileId: %s",
                                    request.operation(), request.fingerprint(), request.fileId()),
                            () -> downloadManifest()
                    );

                    ExecutionProfiling.run(
                            String.format("download-chunks operation: %s fingerprint %s fileId: %s: chunks %s",
                                    request.operation(), request.fingerprint(), request.fileId(), manifest.chunkManifests().size()
                            ),
                            () -> downloadAndWriteChunks()
                    );

                    ExecutionProfiling.run(
                            String.format("digest-calculation operation: %s fingerprint %s fileId: %s",
                                    request.operation(), request.fingerprint(), request.fileId()),
                            () -> {
                                String actualSha256 = fileHelper.sha256(request.temporaryFilePath());
                                if (!manifest.hash().equals(actualSha256)) {
                                    ProcedureExceptions.totalDigestMismatchException(request.operation(), manifest.hash(), actualSha256);
                                }
                            }
                    );

                    isInterrupted();

                    Files.move(request.temporaryFilePath(), request.destinationFilePath(), StandardCopyOption.ATOMIC_MOVE);
                }
        );
    }

    public FileDownloadOrchestrator.DownloadProgress getProgress() {
        if (manifest == null) {
            return new FileDownloadOrchestrator.DownloadProgress(
                    request.operation(),
                    request.fingerprint(),
                    request.fileId(),
                    request.destinationFilePath().toAbsolutePath().toString(),
                    null,
                    0,
                    0,
                    0,
                    CommonUtils.percent(0, 0)
            );
        }

        long totalDownloaded = downloadSpeedMeter.getTotalDownloaded();
        long speedBytesPerSec = downloadSpeedMeter.getSpeedBytesPerSec();
        String percent = CommonUtils.percent(totalDownloaded, manifest.size());

        return new FileDownloadOrchestrator.DownloadProgress(
                request.operation(),
                request.fingerprint(),
                request.fileId(),
                request.destinationFilePath().toAbsolutePath().toString(),
                manifest.hash(),
                manifest.size(),
                totalDownloaded,
                speedBytesPerSec,
                percent
        );
    }

    private void downloadManifest() {
        manifest = RetryExecutor.call(
                        () -> {
                            isInterrupted();
                            int manifestChunkMaxSize = configuration.manifestChunkMaxSize();

                            FileManifest fileManifest = tunnelClient.send(
                                    TunnelClient.Request.builder()
                                            .command("share-download-manifest")
                                            .body(new ShareDownloadManifestCommandRequest(
                                                    request.fileId(),
                                                    manifestChunkMaxSize
                                            ))
                                            .fingerprint(request.fingerprint())
                                            .build(),
                                    FileManifest.class
                            );
                            fileManifestBuilder.validate(fileManifest);
                            return fileManifest;
                        }
                )
                .doOnError((attempt, exception) -> {
                    log.info("Retry 'share-download-manifest'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} exception: {}",
                            request.operation(), request.fingerprint(), request.fileId(), request.filename(), attempt, exception.getMessage(), exception
                    );
                })
                .run();

        RetryExecutor
                .call(() -> {
                    try (FileChannel fileChannel = FileChannel.open(
                            request.manifestFilePath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE)) {
                        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
                        fileHelper.write(fileChannel, bytes, 0);
                    }
                    return 1;
                })
                .doOnError((attempt, exception) -> {
                    log.info("Retry 'write-file-manifest'. Operation: {} fingerprint {} manifest: {} attempt: {} exception: {}",
                            request.operation(), request.fingerprint(), request.manifestFilePath().toAbsolutePath(), attempt, exception.getMessage(), exception
                    );
                })
                .run();
    }

    private void downloadAndWriteChunks() throws Exception {
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();

        try (FileChannel fileChannel = FileChannel.open(
                request.temporaryFilePath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
            List<CompletableFuture<Void>> activeFutures = new ArrayList<>();
            Iterator<ChunkManifest> iterator = manifest.chunkManifests().iterator();
            while (iterator.hasNext()) {
                if (exceptionAtomicReference.get() != null) {
                    throw exceptionAtomicReference.get();
                }
                isInterrupted();
                ChunkManifest chunkManifest = iterator.next();
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> {
                            if (exceptionAtomicReference.get() != null) {
                                return;
                            }
                            try {
                                byte[] chunkBytes = chunkDownload(chunkManifest);
                                downloadSpeedMeter.addChunk(chunkManifest.size());
                                writeChunkToFile(fileChannel, chunkBytes, chunkManifest);
                            } catch (Exception exception) {
                                exceptionAtomicReference.set(exception);
                            }
                        },
                        executorService
                );
                activeFutures.add(future);

                if (!iterator.hasNext()) {
                    CompletableFuture.allOf(activeFutures.toArray(new CompletableFuture[0])).join();
                } else if (activeFutures.size() >= configuration.maxThreadSize()) {
                    CompletableFuture.anyOf(activeFutures.toArray(new CompletableFuture[0])).join();
                    activeFutures.removeIf(it -> it.isDone());
                }
            }
        }
    }

    private byte[] chunkDownload(ChunkManifest chunkManifest) {
        return RetryExecutor.call(
                        () -> {
                            isInterrupted();
                            try (InputStream inputStream = tunnelClient.stream(
                                    TunnelClient.Request.builder()
                                            .command("share-download-chunk-stream")
                                            .body(new ShareDownloadChunkStreamTunnelRequest(
                                                    request.fileId(),
                                                    chunkManifest.size(),
                                                    chunkManifest.position()
                                            ))
                                            .fingerprint(request.fingerprint())
                                            .build())) {
                                byte[] allBytes = inputStream.readNBytes(chunkManifest.size());
                                String sha256 = fileHelper.sha256(allBytes);
                                if (!chunkManifest.hash().equals(sha256)) {
                                    ProcedureExceptions.chunkDigestMismatchException(
                                            request.operation(), chunkManifest.hash(), sha256, chunkManifest.size(), chunkManifest.position()
                                    );
                                }
                                return allBytes;
                            }
                        }
                )
                .doOnError((attempt, exception) -> {
                    log.info("Retry 'share-download-chunk-stream'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} size {} position {} exception: {}",
                            request.operation(), request.fingerprint(), request.fileId(), request.filename(), attempt,
                            chunkManifest.size(), chunkManifest.position(), exception.getMessage(), exception
                    );
                })
                .run();
    }

    private void writeChunkToFile(FileChannel writeToFileChannel,
                                  byte[] chunkBytes,
                                  ChunkManifest chunkManifest) {
        RetryExecutor.call(
                        () -> {
                            isInterrupted();
                            fileHelper.write(writeToFileChannel, chunkBytes, chunkManifest.position());
                            return 1;
                        }
                )
                .doOnError((attempt, exception) -> {
                    log.info("Retry 'write-chunk'. Attempt: {} size {} position {} exception {}",
                            attempt, chunkManifest.size(), chunkManifest.position(), exception.getMessage(), exception
                    );
                })
                .run();
    }

    @SneakyThrows
    private void isInterrupted() {
        CommonUtils.isInterrupted("Downloading process has been interrupted: " + request.operation());
    }
}
