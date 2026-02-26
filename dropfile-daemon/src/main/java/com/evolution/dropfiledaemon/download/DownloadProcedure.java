package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.download.exception.*;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestTunnelResponse;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.FileHelper;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class DownloadProcedure {

    private static final Integer MAX_THREADS = 2;

    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final DownloadSpeedMeter downloadSpeedMeter = new DownloadSpeedMeter();

    private final TunnelClient tunnelClient;

    private final FileHelper fileHelper;

    private final String operation;

    private final String fingerprint;

    private final String fileId;

    private final String filename;

    private final File destinationFile;

    private final File temporaryFile;

    private ExecutorService executorService;

    private ShareDownloadManifestTunnelResponse manifest;

    public boolean isStopped() {
        return isStopped.get();
    }

    @SneakyThrows
    public void stop() {
        synchronized (isStopped) {
            if (isStopped.get()) {
                return;
            }
            isStopped.set(true);
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }
    }

    private ExecutorService getExecutorService() {
        synchronized (isStopped) {
            if (isStopped.get()) {
                throw new IllegalStateException("Download procedure already stopped: " + operation);
            }
            if (this.executorService == null) {
                this.executorService = Executors.newVirtualThreadPerTaskExecutor();
            }
            return executorService;
        }
    }

    public void run() {
        try (ExecutorService executorService = getExecutorService()) {
            this.executorService = executorService;
            CompletableFuture.runAsync(() -> runProcedure(), executorService)
                    .join();
        }
    }

    private void runProcedure() {
        ExecutionProfiling.run(
                String.format("file-download-prodecure operation: %s fingerprint %s fileId: %s", operation,
                        fingerprint, fileId),
                () -> {

                    ExecutionProfiling.run(
                            String.format("download-manifest operation: %s fingerprint %s fileId: %s",
                                    operation, fingerprint, fileId),
                            () -> downloadManifest()
                    );

                    validateManifest(manifest);

                    ExecutionProfiling.run(
                            String.format("download-chunks operation: %s fingerprint %s fileId: %s: chunks %s",
                                    operation, fingerprint, fileId, manifest.chunkManifests().size()
                            ),
                            () -> downloadAndWriteChunks()
                    );

                    String actualSha256 = ExecutionProfiling.run(
                            String.format("digest-calculation operation: %s fingerprint %s fileId: %s",
                                    operation, fingerprint, fileId),
                            () -> fileHelper.sha256(temporaryFile)
                    );

                    if (!manifest.hash().equals(actualSha256)) {
                        throw new TotalDigestMismatchException(operation, manifest.hash(), actualSha256);
                    }

                    isInterrupted();

                    Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                }
        );
    }

    public FileDownloadOrchestrator.DownloadProgress getProgress() {
        if (manifest == null) {
            return new FileDownloadOrchestrator.DownloadProgress(
                    operation,
                    fingerprint,
                    fileId,
                    destinationFile.getAbsolutePath(),
                    null,
                    0,
                    0,
                    0,
                    fileHelper.percent(0, 0)
            );
        }

        long totalDownloaded = downloadSpeedMeter.getTotalDownloaded();
        long speedBytesPerSec = downloadSpeedMeter.getSpeedBytesPerSec();
        String percent = fileHelper.percent(totalDownloaded, manifest.size());

        return new FileDownloadOrchestrator.DownloadProgress(
                operation,
                fingerprint,
                fileId,
                destinationFile.getAbsolutePath(),
                manifest.hash(),
                manifest.size(),
                totalDownloaded,
                speedBytesPerSec,
                percent
        );
    }

    private void downloadManifest() {
        try {
            manifest = RetryExecutor.call(
                            () -> {
                                isInterrupted();
                                return tunnelClient.send(
                                        TunnelClient.Request.builder()
                                                .command("share-download-manifest")
                                                .body(fileId)
                                                .fingerprint(fingerprint)
                                                .build(),
                                        ShareDownloadManifestTunnelResponse.class
                                );
                            }
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'share-download-manifest'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} exception: {}",
                                operation, fingerprint, fileId, filename, attempt, exception.getMessage()
                        );
                    })
                    .run();
        } catch (Exception e) {
            throw new ManifestDownloadingFailedException(operation, fileId, filename, e);
        }
    }

    private void validateManifest(ShareDownloadManifestTunnelResponse manifest) {
        if (ObjectUtils.isEmpty(manifest.chunkManifests())) {
            throw new RuntimeException(String.format(
                    "No chunks in manifest found %s %s", manifest.id(), manifest.hash()
            ));
        }
        List<ShareDownloadManifestTunnelResponse.ChunkManifest> list = manifest.chunkManifests().stream()
                .filter(it -> it.size() > FileManifestBuilder.CHUNK_SIZE)
                .toList();
        if (!list.isEmpty()) {
            throw new RuntimeException(String.format(
                    "Found %s unacceptable chunk size %s %s", list.size(), manifest.id(), manifest.hash()
            ));
        }
    }

    private void downloadAndWriteChunks() throws Exception {
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();

        try (FileChannel fileChannel = FileChannel.open(
                temporaryFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
            List<CompletableFuture<Void>> activeFutures = new ArrayList<>();
            Iterator<ShareDownloadManifestTunnelResponse.ChunkManifest> iterator = manifest.chunkManifests().iterator();
            while (iterator.hasNext()) {
                if (exceptionAtomicReference.get() != null) {
                    throw exceptionAtomicReference.get();
                }
                isInterrupted();
                ShareDownloadManifestTunnelResponse.ChunkManifest chunkManifest = iterator.next();
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
                } else if (activeFutures.size() >= MAX_THREADS) {
                    CompletableFuture.anyOf(activeFutures.toArray(new CompletableFuture[0])).join();
                    activeFutures.removeIf(it -> it.isDone());
                }
            }
        }
    }

    private byte[] chunkDownload(ShareDownloadManifestTunnelResponse.ChunkManifest chunkManifest) {
        try {
            return RetryExecutor.call(
                            () -> {
                                isInterrupted();
                                try (InputStream inputStream = tunnelClient.stream(
                                        TunnelClient.Request.builder()
                                                .command("share-download-chunk-stream")
                                                .body(new ShareDownloadChunkStreamTunnelRequest(
                                                        fileId,
                                                        chunkManifest.startPosition(),
                                                        chunkManifest.endPosition()
                                                ))
                                                .fingerprint(fingerprint)
                                                .build())) {
                                    byte[] allBytes = inputStream.readNBytes(chunkManifest.size());
                                    String sha256 = fileHelper.sha256(allBytes);
                                    if (!chunkManifest.hash().equals(sha256)) {
                                        throw new ChunkDigestMismatchException(operation, chunkManifest.hash(),
                                                sha256, chunkManifest.startPosition(), chunkManifest.endPosition()
                                        );
                                    }
                                    return allBytes;
                                }
                            }
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'share-download-chunk-stream'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} start {} end {} exception: {}",
                                operation, fingerprint, fileId, filename, attempt,
                                chunkManifest.startPosition(), chunkManifest.endPosition(), exception.getMessage()
                        );
                    })
                    .run();
        } catch (Exception e) {
            throw new ChunkDownloadingFailedException(operation, chunkManifest.hash(), chunkManifest.startPosition(), chunkManifest.endPosition(), e);
        }
    }

    private void writeChunkToFile(FileChannel writeToFileChannel,
                                  byte[] chunkBytes,
                                  ShareDownloadManifestTunnelResponse.ChunkManifest chunkManifest) {
        try {
            RetryExecutor.call(
                            () -> {
                                isInterrupted();
                                fileHelper.write(writeToFileChannel, chunkBytes, chunkManifest.startPosition());
                                return chunkBytes;
                            }
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'write-chunk'. Attempt: {} start {} end {} exception {}",
                                attempt, chunkManifest.startPosition(), chunkManifest.endPosition(), exception.getMessage(), exception);
                    })
                    .retryIf(it -> {
                        if (it.exception() instanceof ClosedChannelException) {
                            return false;
                        }
                        return it.exception() != null;
                    })
                    .run();
        } catch (Exception exception) {
            throw new ChunkWritingFailedException(operation, chunkManifest.hash(), chunkManifest.startPosition(), chunkManifest.endPosition(), exception);
        }
    }

    @SneakyThrows
    private void isInterrupted() {
        CommonUtils.isInterrupted("Downloading process has been interrupted: " + operation);
    }
}
