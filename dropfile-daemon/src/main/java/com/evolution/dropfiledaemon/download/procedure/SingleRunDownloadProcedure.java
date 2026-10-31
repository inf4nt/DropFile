package com.evolution.dropfiledaemon.download.procedure;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.function.VoidCallable;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.common.io.ThroughputMeter;
import com.evolution.dropfiledaemon.download.FileDownloadOrchestrator;
import com.evolution.dropfiledaemon.download.procedure.manifest.ChunkManifest;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;
import com.evolution.dropfiledaemon.service.ConcurrentTaskService;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClientGateway;
import com.evolution.dropfiledaemon.util.ExecutionProfiling;
import com.evolution.dropfiledaemon.util.RetryExecutor;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class SingleRunDownloadProcedure {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final ThroughputMeter throughputMeter = new ThroughputMeter();

    private final AtomicBoolean running = new AtomicBoolean();

    private final AtomicBoolean stopped = new AtomicBoolean();

    private final AtomicBoolean terminationInitiated = new AtomicBoolean(false);

    private final CountDownLatch terminationLatch = new CountDownLatch(1);

    private final ConcurrentTaskService concurrentTaskService;

    private final TunnelClientGateway tunnelClientGateway;

    private final FileHelper fileHelper;

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

    public void awaitTermination() {
        if (!stopped.get()) {
            throw new IllegalStateException("Operation not allowed: status is not STOPPED");
        }

        if (terminationInitiated.compareAndSet(false, true)) {
            try {
                concurrentTaskService.awaitUninterruptibly(
                        () -> executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS)
                );
            } finally {
                terminationLatch.countDown();
            }
        } else {
            concurrentTaskService.awaitUninterruptibly(() -> terminationLatch.await());
        }
    }

    public void run(Runnable beforeProcedureCallback,
                    Runnable successCallback) throws ExecutionException {
        if (stopped.get()) {
            throw new IllegalStateException(
                    "Download procedure was forcibly stopped: " + request.operationId()
            );
        }

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "Download procedure is running: " + request.operationId()
            );
        }

        try (ExecutorService service = executorService) {
            Future<?> future = service.submit(() -> {
                beforeProcedureCallback.run();
                runProcedure();
                successCallback.run();
            });

            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Execution was interrupted", e);
            }
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
