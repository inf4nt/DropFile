package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.ThrowableUtils;
import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.download.procedure.DownloadProcedureFactory;
import com.evolution.dropfiledaemon.download.procedure.DownloadProcedureRequest;
import com.evolution.dropfiledaemon.download.procedure.SingleRunDownloadProcedure;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifest;
import com.evolution.dropfiledaemon.download.procedure.manifest.FileManifestService;
import com.evolution.dropfiledaemon.service.SafePathResolverHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
@Component
public class FileDownloadOrchestrator {

    private final ExecutorService fileDownloadingExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, SingleRunDownloadProcedure> downloadProcedures = new ConcurrentHashMap<>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final DownloadProcedureFactory downloadProcedureFactory;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final FileDownloadStore fileDownloadStore;

    private final DirectoryProvider daemonDownloadsDirectoryProvider;

    private final FileManifestService fileManifestService;

    private final SafePathResolverHelper safePathResolverHelper;

    public FileDownloadResponse start(FileDownloadRequest request) {
        String filename = safePathResolverHelper.sanitizeFilename(request.filename());
        request = request.withFilename(filename);

        return doStart(request);
    }

    private FileDownloadResponse doStart(FileDownloadRequest request) {
        SingleRunDownloadProcedure downloadProcedure;

        synchronized (this) {
            checkIfClosed();

            int maxActiveDownloads = daemonApplicationProperties.daemonDownloadOrchestratorActiveQueueSize;
            int activeDownloads = downloadProcedures.size();
            if (activeDownloads >= maxActiveDownloads) {
                throw new IllegalStateException(
                        "No available permits. Current: %s total: %s"
                                .formatted(activeDownloads, maxActiveDownloads)
                );
            }

            Path destinationFilePath = getDestinationFilePath(request);
            Path temporaryFilePath = getTemporaryFilePath(destinationFilePath);

            String operationId = CommonUtils.generateId();
            FileManifest fileManifest = fileManifestService.build(request.hash(), request.size());
            downloadProcedure = downloadProcedureFactory.get(
                    operationId,
                    request.fingerprint(),
                    request.fileId(),
                    request.filename(),
                    fileManifest,
                    destinationFilePath,
                    temporaryFilePath
            );

            downloadProcedures.put(operationId, downloadProcedure);
        }

        runDownload(downloadProcedure);

        return new FileDownloadResponse(
                downloadProcedure.getRequest().operationId(),
                downloadProcedure.getRequest().fileId(),
                downloadProcedure.getRequest().destinationFilePath().toAbsolutePath().toString()
        );
    }

    private void runDownload(SingleRunDownloadProcedure downloadProcedure) {
        DownloadProcedureRequest request = downloadProcedure.getRequest();

        String operationId = request.operationId();
        String fingerprint = request.fingerprint();
        String fileId = request.fileId();
        Path destinationFilePath = request.destinationFilePath();
        Path temporaryFilePath = request.temporaryFilePath();

        long fileSize = request.fileManifest().size();
        String fileHash = request.fileManifest().hash();

        try {
            fileDownloadingExecutorService.execute(() -> {
                try {
                    checkIfClosed();

                    downloadProcedure.run(
                            () -> fileDownloadStore.save(
                                    operationId,
                                    () -> {
                                        Instant createInstantTime = Instant.now();
                                        return new DownloadFile(
                                                fingerprint,
                                                fileId,
                                                destinationFilePath.toAbsolutePath().toString(),
                                                temporaryFilePath.toAbsolutePath().toString(),
                                                fileHash,
                                                fileSize,
                                                DownloadFile.DownloadFileStatus.DOWNLOADING,
                                                createInstantTime,
                                                createInstantTime
                                        );
                                    }
                            ),
                            () -> fileDownloadStore.update(
                                    operationId,
                                    downloadFileEntry -> downloadFileEntry
                                            .withDownloaded(downloadProcedure.getProgress().downloaded())
                                            .withStatus(DownloadFile.DownloadFileStatus.COMPLETED)
                                            .withUpdated(Instant.now())
                            )
                    );
                } catch (Exception exception) {
                    if (downloadProcedure.isStopped()) {
                        log.info(
                                "Download operationId {} (fingerprint {}) was stopped by user request.",
                                operationId,
                                fingerprint
                        );
                        return;
                    }

                    log.error(
                            "Exception occurred during download process operationId {} fingerprint {} {}",
                            operationId,
                            fingerprint,
                            exception.getMessage(),
                            exception
                    );

                    fileDownloadStore.update(
                            operationId,
                            downloadFile -> downloadFile
                                    .withDownloaded(downloadProcedure.getProgress().downloaded())
                                    .withStatus(DownloadFile.DownloadFileStatus.ERROR)
                                    .withUpdated(Instant.now())
                    );
                } finally {
                    downloadProcedures.remove(operationId);
                    CommonUtils.executeSafety(() -> Files.deleteIfExists(temporaryFilePath));
                }
            });
        } catch (Exception e) {
            log.error(
                    "Error during starting download process operationId {} {}",
                    operationId,
                    e.getMessage(),
                    e
            );

            downloadProcedures.remove(operationId);
            CommonUtils.executeSafety(() -> Files.deleteIfExists(temporaryFilePath));

            throw ThrowableUtils.rethrowRuntimeException(e);
        }
    }

    public Map<String, DownloadProgress> getDownloadProcedures() {
        return downloadProcedures.entrySet().stream()
                .collect(Collectors.toMap(
                        it -> it.getKey(),
                        it -> it.getValue().getProgress()
                ));
    }

    public FileDownloadOrchestratorKillResponse kill(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        if (CollectionUtils.isEmpty(operationIdCriteriaEnvelopes)) {
            return new FileDownloadOrchestratorKillResponse(Map.of(), List.of(), Map.of());
        }

        Set<String> allOperations = Stream.concat(
                        downloadProcedures.keySet().stream(),
                        fileDownloadStore.getAll().keySet().stream()
                )
                .collect(Collectors.toSet());

        CommonUtils.MatchResult<String> matchResult = CommonUtils.matchBy(
                allOperations,
                operationIdCriteriaEnvelopes,
                (criteria, operationId) -> operationId.startsWith(criteria.value())
        );

        Map<String, SingleRunDownloadProcedure> targetOperations = matchResult.found().values()
                .stream()
                .map(downloadProcedures::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        x -> x.getRequest().operationId(),
                        x -> x
                ));

        stopProcedures(targetOperations);

        Map<String, DownloadFile> removed = fileDownloadStore.remove(matchResult.found().values());
        deleteFilesBatch(removed.values());

        return new FileDownloadOrchestratorKillResponse(
                matchResult.found(),
                matchResult.notFound(),
                matchResult.ambiguous()
        );
    }

    public void killAll() {
        Map<String, SingleRunDownloadProcedure> targetOperations;
        Set<String> keysToKill;

        synchronized (this) {
            targetOperations = Map.copyOf(downloadProcedures);
            downloadProcedures.clear();

            keysToKill = Stream.concat(
                            targetOperations.keySet().stream(),
                            fileDownloadStore.getAll().keySet().stream()
                    )
                    .collect(Collectors.toSet());
        }

        stopProcedures(targetOperations);

        Map<String, DownloadFile> removed = fileDownloadStore.remove(keysToKill);

        deleteFilesBatch(removed.values());
    }

    private void deleteFilesBatch(Collection<DownloadFile> files) {
        List<Exception> suppressedExceptions = new ArrayList<>();

        for (DownloadFile file : files) {
            try {
                Path path = Paths.get(file.destinationFile());

                if (Files.exists(path) && !Files.isRegularFile(path)) {
                    throw new IllegalArgumentException(
                            "File is not a regular file. Unable to remove %s".formatted(path)
                    );
                }

                Files.deleteIfExists(path);
            } catch (Exception e) {
                suppressedExceptions.add(e);
            }
        }

        if (!suppressedExceptions.isEmpty()) {
            IOException aggregate = new IOException("Failed to delete some files in batch");
            suppressedExceptions.forEach(aggregate::addSuppressed);
            log.error("Batch deletion completed with errors", aggregate);
        }
    }

    public void stopAll() {
        Map<String, SingleRunDownloadProcedure> operations;

        synchronized (this) {
            operations = Map.copyOf(downloadProcedures);
            downloadProcedures.clear();
        }

        stopProcedures(operations);
    }

    private void stopProcedures(Map<String, SingleRunDownloadProcedure> operations) {
        if (ObjectUtils.isEmpty(operations)) {
            return;
        }

        operations.values().forEach(it -> CommonUtils.executeSafety(() -> it.stop()));

        fileDownloadStore.save(
                () -> {
                    Instant now = Instant.now();

                    return operations.entrySet()
                            .stream()
                            .map(downloadProcedureEntry -> {
                                String operationId = downloadProcedureEntry.getKey();
                                SingleRunDownloadProcedure downloadProcedure = downloadProcedureEntry.getValue();

                                DownloadFile downloadFile = fileDownloadStore.get(operationId)
                                        .map(Map.Entry::getValue)
                                        .map(current -> {
                                            DownloadProgress progress = downloadProcedure.getProgress();
                                            return current.withStatus(DownloadFile.DownloadFileStatus.STOPPED)
                                                    .withUpdated(now)
                                                    .withDownloaded(progress.downloaded());
                                        })
                                        .orElseGet(() -> {
                                            DownloadProcedureRequest request = downloadProcedure.getRequest();
                                            return new DownloadFile(
                                                    request.fingerprint(),
                                                    request.fileId(),
                                                    request.destinationFilePath().toAbsolutePath().toString(),
                                                    request.temporaryFilePath().toAbsolutePath().toString(),
                                                    request.fileManifest().hash(),
                                                    request.fileManifest().size(),
                                                    DownloadFile.DownloadFileStatus.STOPPED,
                                                    now,
                                                    now
                                            );
                                        });

                                return Map.entry(operationId, downloadFile);
                            })
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue
                            ));
                },
                KeyValueStore.ValidatePolicy.GENTLE
        );
    }

    private Path getDestinationFilePath(FileDownloadRequest request) {
        Path downloadDirectoryPath = daemonDownloadsDirectoryProvider.getDirectoryPath();
        Path downloadFilePath = downloadDirectoryPath.resolve(request.filename()).normalize();

        if (!downloadFilePath.startsWith(downloadDirectoryPath)) {
            throw new SecurityException(
                    "Path traversal attempt detected: " + request.filename()
            );
        }

        downloadProcedures.entrySet().stream()
                .filter(entry -> downloadFilePath.toAbsolutePath()
                        .equals(entry.getValue().getRequest().destinationFilePath().toAbsolutePath()))
                .findAny()
                .ifPresent(duplicate -> {
                    throw new IllegalStateException(
                            "File download request is already running operationId %s file %s"
                                    .formatted(
                                            duplicate.getKey(),
                                            duplicate.getValue().getRequest().destinationFilePath()
                                    )
                    );
                });

        if (Files.exists(downloadFilePath)) {
            throw new IllegalStateException(
                    "File download request failed. File already exists: %s"
                            .formatted(downloadFilePath)
            );
        }

        return downloadFilePath;
    }

    private Path getTemporaryFilePath(Path destinationFilePath) {
        Path downloadDirectoryPath = daemonDownloadsDirectoryProvider.getDirectoryPath();

        String safeFilename = destinationFilePath.getFileName().toString();
        String temporaryFileName = CommonFileUtils.getTemporaryFileName(safeFilename);

        Path temporaryFile = downloadDirectoryPath.resolve(temporaryFileName);

        if (!temporaryFile.startsWith(downloadDirectoryPath)) {
            throw new SecurityException(
                    "Path traversal attempt detected: " + temporaryFile
            );
        }

        if (Files.exists(temporaryFile)) {
            throw new IllegalStateException(
                    "File already exists: %s".formatted(temporaryFile)
            );
        }

        return temporaryFile;
    }

    @EventListener(ContextClosedEvent.class)
    public void contextClosedEventListener() throws InterruptedException {
        boolean set = closed.compareAndSet(false, true);
        if (!set) {
            return;
        }

        log.info(
                "Closing {} by {}",
                FileDownloadOrchestrator.class,
                ContextClosedEvent.class
        );

        log.info("Shutdown main executor service");
        fileDownloadingExecutorService.shutdown();
        log.info("Shutdown main executor service completed");

        log.info("Stop All download procedures");
        stopAll();
        log.info("Stop All download procedures completed");

        log.info("AwaitTermination main executor service");
        boolean finishedCleanly = fileDownloadingExecutorService.awaitTermination(
                10,
                TimeUnit.SECONDS
        );
        log.info(
                "AwaitTermination main executor service completed. Result {}",
                finishedCleanly
        );

        if (!finishedCleanly) {
            log.info("ShutdownNow main executor service");
            fileDownloadingExecutorService.shutdownNow();
            log.info("ShutdownNow main executor service completed");
        }

        log.info("Closed");
    }

    private void checkIfClosed() {
        if (closed.get()) {
            throw new IllegalStateException(
                    "Already closed " + FileDownloadOrchestrator.class
            );
        }
    }

    // TODO add ETA
    public record DownloadProgress(
            String operationId,
            String fingerprint,
            String fileId,
            String filename,
            String hash,
            long total,
            long downloaded,
            long speedBytesPerSec,
            String percentage
    ) {
    }

    public record FileDownloadOrchestratorKillResponse(
            Map<CriteriaEnvelope, String> found,
            Collection<CriteriaEnvelope> notFound,
            Map<CriteriaEnvelope, List<String>> ambiguous
    ) {
    }
}
