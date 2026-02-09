package com.evolution.dropfiledaemon.download;

import com.evolution.dropfiledaemon.download.exception.ChunkDigestMismatchException;
import com.evolution.dropfiledaemon.download.exception.ChunkDownloadingFailedException;
import com.evolution.dropfiledaemon.download.exception.ChunkWritingFailedException;
import com.evolution.dropfiledaemon.download.exception.DownloadingStoppedException;
import com.evolution.dropfiledaemon.download.exception.ManifestDownloadingFailedException;
import com.evolution.dropfiledaemon.download.exception.TotalDigestMismatchException;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestTunnelResponse;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.FileHelper;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import com.evolution.dropfiledaemon.util.SafeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@RequiredArgsConstructor
public class DownloadProcedure {

    private final AtomicBoolean stop = new AtomicBoolean(false);

    private final LongAdder chunksHaveDownloaded = new LongAdder();

    private final Semaphore chunkDownloadingSemaphore;

    private final ExecutorService executorService;

    private final TunnelClient tunnelClient;

    private final FileHelper fileHelper;

    private final String operationId;

    private final FileDownloadRequest request;

    private final File destinationFile;

    private final File temporaryFile;

    private ShareDownloadManifestTunnelResponse manifest;

    public void stop() {
        stop.set(true);
    }

    public void run() {
        try {
            ExecutionProfiling.run(
                    String.format("file-download-prodecure operation: %s fileId: %s", operationId, request.fileId()),
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
                            throw new TotalDigestMismatchException(operationId, manifest.hash(), actualSha256);
                        }

                        checkIfProcessHasStopped();

                        Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                    }
            );
        } finally {
            if (temporaryFile != null && Files.exists(temporaryFile.toPath())) {
                SafeUtils.execute(() -> Files.delete(temporaryFile.toPath()));
            }
        }
    }

    public FileDownloadOrchestrator.DownloadProgress getProgress() {
        if (manifest == null) {
            return new FileDownloadOrchestrator.DownloadProgress(
                    operationId,
                    request.fileId(),
                    destinationFile.getAbsolutePath(),
                    null,
                    0,
                    0,
                    fileHelper.percent(0, 0)
            );
        }
        long downloaded = chunksHaveDownloaded.sum();
        String percent = fileHelper.percent(downloaded, manifest.size());
        return new FileDownloadOrchestrator.DownloadProgress(
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
            manifest = RetryExecutor.call(
                            () -> {
                                checkIfProcessHasStopped();
                                return tunnelClient.send(
                                        TunnelClient.Request.builder()
                                                .command("share-download-manifest")
                                                .body(request.fileId())
                                                .build(),
                                        ShareDownloadManifestTunnelResponse.class);
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

    private void downloadAndWriteChunks() throws Exception {
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try (FileChannel fileChannel = FileChannel.open(
                temporaryFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
            for (ShareDownloadManifestTunnelResponse.ChunkManifest chunkManifest : manifest.chunkManifests()) {
                chunkDownloadingSemaphore.acquire();
                if (exceptionAtomicReference.get() != null) {
                    throw exceptionAtomicReference.get();
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> {
                            if (exceptionAtomicReference.get() != null) {
                                return;
                            }
                            try {
                                byte[] chunkBytes = chunkDownload(chunkManifest);
                                writeChunkToFile(fileChannel, chunkBytes, chunkManifest);
                                chunksHaveDownloaded.add(chunkManifest.size());
                            } catch (Exception exception) {
                                exceptionAtomicReference.set(exception);
                            } finally {
                                SafeUtils.execute(() -> chunkDownloadingSemaphore.release());
                            }
                        },
                        executorService
                );
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    private byte[] chunkDownload(ShareDownloadManifestTunnelResponse.ChunkManifest chunkManifest) {
        try {
            return RetryExecutor.call(
                            () -> {
                                checkIfProcessHasStopped();
                                try (InputStream inputStream = tunnelClient.stream(
                                        TunnelClient.Request.builder()
                                                .command("share-download-chunk-stream")
                                                .body(new ShareDownloadChunkStreamTunnelRequest(
                                                        request.fileId(),
                                                        chunkManifest.startPosition(),
                                                        chunkManifest.endPosition()
                                                ))
                                                .build())) {
                                    byte[] allBytes = inputStream.readNBytes(chunkManifest.size());
                                    String sha256 = fileHelper.sha256(allBytes);
                                    if (!chunkManifest.hash().equals(sha256)) {
                                        throw new ChunkDigestMismatchException(operationId, chunkManifest.hash(),
                                                sha256, chunkManifest.startPosition(), chunkManifest.endPosition()
                                        );
                                    }
                                    return allBytes;
                                }
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

    private void writeChunkToFile(FileChannel writeToFileChannel,
                                  byte[] chunkBytes,
                                  ShareDownloadManifestTunnelResponse.ChunkManifest chunkManifest) {
        try {
            RetryExecutor.call(
                            () -> {
                                checkIfProcessHasStopped();
                                fileHelper.write(writeToFileChannel, chunkBytes, chunkManifest.startPosition());
                                return null;
                            }
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'write-chunk'. Attempt: {} start {} end {} exception {}",
                                attempt, chunkManifest.startPosition(), chunkManifest.endPosition(), exception.getMessage());
                    })
                    .retryIf(it -> {
                        if (it.exception() instanceof DownloadingStoppedException) {
                            return false;
                        }
                        return it.exception() != null;
                    })
                    .build()
                    .run();
        } catch (Exception exception) {
            throw new ChunkWritingFailedException(operationId, chunkManifest.hash(), chunkManifest.startPosition(), chunkManifest.endPosition(), exception);
        }
    }

    private void checkIfProcessHasStopped() {
        if (stop.get()) {
            throw new DownloadingStoppedException(operationId);
        }
    }
}
