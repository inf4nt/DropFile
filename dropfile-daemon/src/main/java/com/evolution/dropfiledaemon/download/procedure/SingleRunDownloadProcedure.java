package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.function.VoidCallable;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.common.io.ThroughputMeter;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.procedure.manifest.ChunkManifest;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;
import com.evolution.dropfiledaemon.concurent.ConcurrentTaskService;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.concurent.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class SingleRunDownloadProcedure {

    private final ThroughputMeter throughputMeter = new ThroughputMeter();

    private final AtomicBoolean startInitiated = new AtomicBoolean();

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final ConcurrentTaskService concurrentTaskService;

    private final TunnelClientGateway tunnelClientGateway;

    private final FileHelper fileHelper;

    private final DownloadProcedureRequest request;

    volatile private Thread executionThread;

    public DownloadProcedureRequest getRequest() {
        return request;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public void stop() {
        synchronized (this) {
            if (stopped.compareAndSet(false, true)) {
                if (executionThread != null) {
                    executionThread.interrupt();
                }
            }
        }
    }

    public void awaitTermination() {
        Thread thread;

        synchronized (this) {
            thread = executionThread;

            if (thread == null) {
                if (!stopped.get()) {
                    throw new IllegalStateException(
                            "Procedure was not started or stopped"
                    );
                }
                return;
            }
        }

        concurrentTaskService.awaitUninterruptibly(thread::join);
    }

    public void start(Runnable beforeProcedureCallback,
                      Runnable successCallback) throws ExecutionException {
        AtomicReference<Throwable> executionThrowable = new AtomicReference<>();
        synchronized (this) {
            if (stopped.get()) {
                throw new IllegalStateException(
                        "Download procedure was forcibly stopped: %s"
                                .formatted(request.operationId())
                );
            }

            if (!startInitiated.compareAndSet(false, true)) {
                throw new IllegalStateException(
                        "Download procedure is one-shot and has already been started: %s"
                                .formatted(request.operationId())
                );
            }

            executionThread = Thread.ofVirtual()
                    .name("download-procedure-" + request.operationId())
                    .unstarted(() -> {
                        try {
                            beforeProcedureCallback.run();
                            runProcedure();
                            successCallback.run();
                        } catch (Throwable throwable) {
                            executionThrowable.set(throwable);
                        }
                    });

            executionThread.start();
        }

        try {
            executionThread.join();
        } catch (InterruptedException e) {
            stop();
            awaitTermination();

            Thread.currentThread().interrupt();
            throw new IllegalStateException("Execution was interrupted", e);
        }

        Throwable throwable = executionThrowable.get();
        if (throwable != null) {
            if (throwable instanceof ExecutionException e) {
                throw e;
            }
            throw new ExecutionException(throwable.getMessage(), throwable);
        }
    }

    private void runProcedure() {
        ExecutionProfiling.run(
                String.format("file-download-procedure operationId: %s fingerprint %s fileId: %s",
                        request.operationId(), request.fingerprint(), request.fileId()),
                () -> {
                    ExecutionProfiling.run(
                            String.format("download-chunks operationId: %s fingerprint %s fileId: %s: chunks %s",
                                    request.operationId(), request.fingerprint(), request.fileId(), request.fileManifest().chunks().size()
                            ),
                            () -> chunksHandler()
                    );

                    ExecutionProfiling.run(
                            String.format("digest-calculation operationId: %s fingerprint %s fileId: %s",
                                    request.operationId(), request.fingerprint(), request.fileId()),
                            () -> totalDigestHandler()
                    );

                    validateInterrupted();

                    atomicMove();
                }
        );
    }


    private void atomicMove() throws IOException {
        // Best-effort no-replace check.
        // Java NIO does not provide an atomic "rename if absent" operationId across all platforms.
        // A race with external processes is still possible between exists() and ATOMIC_MOVE.
        if (Files.exists(request.destinationFilePath())) {
            throw new FileAlreadyExistsException("Unable to execute atomic_move. Destination file already exists %s"
                    .formatted(request.destinationFilePath()));
        }
        Files.move(
                request.temporaryFilePath(),
                request.destinationFilePath(),
                StandardCopyOption.ATOMIC_MOVE
        );
    }

    public FileDownloadOrchestrator.DownloadProgress getProgress() {
        FileManifest manifest = request.fileManifest();
        long totalDownloaded = throughputMeter.getTotalThroughput();
        long speedBytesPerSec = throughputMeter.getSpeedBytesPerSec();
        String percent = CommonUtils.percent(totalDownloaded, manifest.size());

        return new FileDownloadOrchestrator.DownloadProgress(
                request.operationId(),
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

    private void chunksHandler() throws IOException, ExecutionException {
        boolean compressible = FileCompressionPolicyHelper.isCompressible(request.filename());

        try (FileChannel fileChannel = FileChannel.open(request.temporaryFilePath(),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
            List<VoidCallable> tasks = request.fileManifest().chunks()
                    .stream()
                    .map(chunkManifest -> (VoidCallable) () -> {
                        handleSingleChunk(fileChannel, chunkManifest, compressible);
                        throughputMeter.add(chunkManifest.size());
                    })
                    .toList();
            concurrentTaskService.executeFailFastDownloadProcedure(tasks);
        }
    }

    private void handleSingleChunk(FileChannel writeToFileChannel, ChunkManifest chunkManifest, boolean compressible) throws ExecutionException {
        RetryExecutor
                .call(() -> {
                    validateInterrupted();
                    try (InputStream stream = tunnelClientGateway.shareDownloadChunkStream(request.fingerprint(),
                            request.fileId(),
                            chunkManifest.size(),
                            chunkManifest.position(),
                            compressible)) {
                        fileHelper.write(writeToFileChannel, stream, chunkManifest.position(), chunkManifest.size());
                    }
                    return 1;
                })
                .doOnError((attempt, exception) -> {
                    log.error("Retry 'share-download-chunk-stream'. Operation: {} fingerprint {} fileId: {} filename: {} attempt: {} size {} position {} exception: {}",
                            request.operationId(), request.fingerprint(), request.fileId(), request.filename(), attempt,
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
                    request.operationId(),
                    manifest.hash(),
                    actualSha256
            ));
        }
    }

    private void validateInterrupted() throws InterruptedException {
        CommonUtils.validateInterrupted("Downloading process has been interrupted: " + request.operationId());
    }
}
