package com.evolution.dropfiledaemon.download;

import com.evolution.dropfiledaemon.download.exception.*;
import com.evolution.dropfiledaemon.manifest.FileManifestBuilder;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadChunkStreamTunnelRequest;
import com.evolution.dropfiledaemon.tunnel.share.dto.ShareDownloadManifestTunnelResponse;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.FileHelper;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import com.evolution.dropfiledaemon.util.SafeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

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

@Slf4j
@RequiredArgsConstructor
public class DownloadProcedure {

    private final AtomicBoolean stop = new AtomicBoolean(false);

    private final DownloadSpeedMeter downloadSpeedMeter = new DownloadSpeedMeter();

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
        ExecutionProfiling.run(
                String.format("file-download-prodecure operation: %s fingerprint %s fileId: %s", operationId,
                        request.fingerprint(), request.fileId()),
                () -> {

                    ExecutionProfiling.run(
                            String.format("download-manifest operation: %s fingerprint %s fileId: %s",
                                    operationId, request.fingerprint(), request.fileId()),
                            () -> downloadManifest()
                    );

                    validateManifest(manifest);

                    ExecutionProfiling.run(
                            String.format("download-chunks operation: %s fingerprint %s fileId: %s: chunks %s",
                                    operationId, request.fingerprint(), request.fileId(), manifest.chunkManifests().size()
                            ),
                            () -> downloadAndWriteChunks()
                    );

                    String actualSha256 = ExecutionProfiling.run(
                            String.format("digest-calculation operation: %s fingerprint %s fileId: %s",
                                    operationId, request.fingerprint(), request.fileId()),
                            () -> fileHelper.sha256(temporaryFile)
                    );

                    if (!manifest.hash().equals(actualSha256)) {
                        throw new TotalDigestMismatchException(operationId, manifest.hash(), actualSha256);
                    }

                    checkIfProcessHasStopped();

                    Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                }
        );
    }

    public FileDownloadOrchestrator.DownloadProgress getProgress() {
        if (manifest == null) {
            return new FileDownloadOrchestrator.DownloadProgress(
                    operationId,
                    request.fingerprint(),
                    request.fileId(),
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
                operationId,
                request.fingerprint(),
                request.fileId(),
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
                                checkIfProcessHasStopped();
                                return tunnelClient.send(
                                        TunnelClient.Request.builder()
                                                .command("share-download-manifest")
                                                .body(request.fileId())
                                                .fingerprint(request.fingerprint())
                                                .build(),
                                        ShareDownloadManifestTunnelResponse.class);
                            }
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'share-download-manifest'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} exception: {}",
                                operationId, request.fingerprint(), request.fileId(), request.filename(), attempt, exception.getMessage()
                        );
                    })
                    .retryIf(it -> {
                        if (it.exception() instanceof DownloadingStoppedException || it.exception() instanceof InterruptedException) {
                            return false;
                        }
                        return it.exception() != null || it.result() == null;
                    })
                    .run();
        } catch (Exception e) {
            throw new ManifestDownloadingFailedException(operationId, request.fileId(), request.filename(), e);
        }
    }

    private void validateManifest(ShareDownloadManifestTunnelResponse manifest) {
        if (ObjectUtils.isEmpty(manifest.chunkManifests())) {
            throw new RuntimeException("No chunks in manifest found");
        }
        List<ShareDownloadManifestTunnelResponse.ChunkManifest> list = manifest.chunkManifests().stream()
                .filter(it -> it.size() > FileManifestBuilder.CHUNK_SIZE)
                .toList();
        if (!list.isEmpty()) {
            throw new RuntimeException(String.format(
                    "Found %s unacceptable chunk size", list.size()
            ));
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
                                downloadSpeedMeter.addChunk(chunkManifest.size());
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
                                                .fingerprint(request.fingerprint())
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
                        log.info("Retry 'share-download-chunk-stream'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} start {} end {} exception: {}",
                                operationId, request.fingerprint(), request.fileId(), request.filename(), attempt,
                                chunkManifest.startPosition(), chunkManifest.endPosition(), exception.getMessage()
                        );
                    })
                    .retryIf(it -> {
                        if (it.exception() instanceof DownloadingStoppedException
                                || it.exception() instanceof InterruptedException) {
                            return false;
                        }
                        return it.exception() != null;
                    })
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
                                return chunkBytes;
                            }
                    )
                    .doOnError((attempt, exception) -> {
                        log.info("Retry 'write-chunk'. Attempt: {} start {} end {} exception {}",
                                attempt, chunkManifest.startPosition(), chunkManifest.endPosition(), exception.getMessage());
                    })
                    .retryIf(it -> {
                        if (it.exception() instanceof DownloadingStoppedException
                                || it.exception() instanceof InterruptedException) {
                            return false;
                        }
                        return it.exception() != null;
                    })
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
