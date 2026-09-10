package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.common.io.ThroughputMeter;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.procedure.manifest.ChunkManifest;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
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
public class SingleRunDownloadProcedure {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final ThroughputMeter throughputMeter = new ThroughputMeter();

    private final AtomicBoolean running = new AtomicBoolean();

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final TunnelClientGateway tunnelClientGateway;

    private final FileHelper fileHelper;

    private final DownloadProcedureConfiguration configuration;

    private final DownloadProcedureRequest request;

    public DownloadProcedureRequest getRequest() {
        return request;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            executorService.shutdownNow();
        }
    }

    public void run(Runnable beforeProcedureCallback,
                    Runnable successCallback) {
        if (stopped.get()) {
            throw new IllegalStateException("Download procedure was forcibly stopped: " + request.operation());
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Download procedure is running: " + request.operation());
        }

        try (ExecutorService service = executorService) {
            CompletableFuture.runAsync(
                            () -> {
                                beforeProcedureCallback.run();
                                runProcedure();
                                successCallback.run();
                            },
                            service
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
                            String.format("download-chunks operation: %s fingerprint %s fileId: %s: chunks %s",
                                    request.operation(), request.fingerprint(), request.fileId(), request.fileManifest().chunks().size()
                            ),
                            () -> chunksHandler()
                    );

                    ExecutionProfiling.run(
                            String.format("digest-calculation operation: %s fingerprint %s fileId: %s",
                                    request.operation(), request.fingerprint(), request.fileId()),
                            () -> totalDigestHandler()
                    );

                    isInterrupted();

                    Files.move(
                            request.temporaryFilePath(),
                            request.destinationFilePath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
        );
    }

    public FileDownloadOrchestrator.DownloadProgress getProgress() {
        FileManifest manifest = request.fileManifest();
        long totalDownloaded = throughputMeter.getTotalThroughput();
        long speedBytesPerSec = throughputMeter.getSpeedBytesPerSec();
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

    private void chunksHandler() throws Exception {
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();

        try (FileChannel fileChannel = FileChannel.open(
                request.temporaryFilePath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            List<CompletableFuture<Void>> activeFutures = new ArrayList<>();
            Iterator<ChunkManifest> iterator = request.fileManifest().chunks().iterator();
            while (iterator.hasNext() && exceptionAtomicReference.get() == null) {
                isInterrupted();
                ChunkManifest chunkManifest = iterator.next();
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> {
                            if (exceptionAtomicReference.get() != null) {
                                return;
                            }
                            try {
                                handleSingleChunk(fileChannel, chunkManifest);
                                throughputMeter.add(chunkManifest.size());
                            } catch (Exception exception) {
                                exceptionAtomicReference.compareAndSet(null, exception);
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

            if (!activeFutures.isEmpty()) {
                CompletableFuture.allOf(activeFutures.toArray(new CompletableFuture[0])).join();
            }

            if (exceptionAtomicReference.get() != null) {
                throw exceptionAtomicReference.get();
            }
        }
    }

    private void handleSingleChunk(FileChannel writeToFileChannel, ChunkManifest chunkManifest) {
        RetryExecutor
                .call(() -> {
                    isInterrupted();
                    try (InputStream stream = tunnelClientGateway.shareDownloadChunkStream(request.fingerprint(),
                            request.fileId(),
                            chunkManifest.size(),
                            chunkManifest.position())) {
                        fileHelper.write(writeToFileChannel, stream, chunkManifest.position(), chunkManifest.size());
                    }
                    return 1;
                })
                .doOnError((attempt, exception) -> {
                    log.error("Retry 'share-download-chunk-stream'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} size {} position {} exception: {}",
                            request.operation(), request.fingerprint(), request.fileId(), request.filename(), attempt,
                            chunkManifest.size(), chunkManifest.position(), exception.getMessage(), exception
                    );
                })
                .run();
    }

    private void totalDigestHandler() throws NoSuchAlgorithmException, IOException {
        String actualSha256 = fileHelper.sha256(request.temporaryFilePath());
        FileManifest manifest = request.fileManifest();
        if (!manifest.hash().equals(actualSha256)) {
            throw new SecurityException(String.format(
                    "Total digest mismatch. Operation: %s expected: %s actual: %s",
                    request.operation(),
                    manifest.hash(),
                    actualSha256
            ));
        }
    }

    @SneakyThrows
    private void isInterrupted() {
        CommonUtils.isInterrupted("Downloading process has been interrupted: " + request.operation());
    }
}
