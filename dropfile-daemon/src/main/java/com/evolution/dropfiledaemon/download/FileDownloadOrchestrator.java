package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.download.procedure.DownloadProcedureFactory;
import com.evolution.dropfiledaemon.download.procedure.SingleRunDownloadProcedure;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
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

    private final Map<String, SingleRunDownloadProcedure> downloadProcedures = new LinkedHashMap<>();

    private final ArrayDeque<Map.Entry<String, SingleRunDownloadProcedure>> waitingQueue = new ArrayDeque<>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final DownloadProcedureFactory downloadProcedureFactory;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final FileDownloadStore fileDownloadStore;

    private final DirectoryProvider daemonDownloadsDirectoryProvider;

    @SneakyThrows
    public FileDownloadResponse start(FileDownloadRequest request) {
        int downloadOrchestratorMaxQueueSize = daemonApplicationProperties.daemonDownloadOrchestratorMaxQueueSize;
        SingleRunDownloadProcedure downloadProcedure;
        synchronized (this) {
            checkIfClosed();

            int currentPermits = downloadProcedures.size() + waitingQueue.size();
            if (currentPermits >= downloadOrchestratorMaxQueueSize) {
                throw new IllegalStateException(
                        "No available permits. Current: %s total: %s".formatted(currentPermits, downloadOrchestratorMaxQueueSize)
                );
            }

            Path destinationFilePath = getDestinationFilePath(request);
            Path manifestFilePath = getManifestFilePath(destinationFilePath);
            Path temporaryFilePath = getTemporaryFilePath(request);

            String operationId = CommonUtils.random();
            downloadProcedure = downloadProcedureFactory.get(
                    operationId,
                    request.fingerprint(),
                    request.fileId(),
                    request.filename(),
                    destinationFilePath,
                    temporaryFilePath,
                    manifestFilePath
            );
            waitingQueue.add(Map.entry(operationId, downloadProcedure));
        }

        tryToStartNext();

        return new FileDownloadResponse(
                downloadProcedure.getRequest().operation(),
                downloadProcedure.getRequest().fileId(),
                downloadProcedure.getRequest().destinationFilePath().toAbsolutePath().toString()
        );
    }

    private void tryToStartNext() {
        int activeQueueSize = daemonApplicationProperties.daemonDownloadOrchestratorActiveQueueSize;
        Map<String, SingleRunDownloadProcedure> toStart = new LinkedHashMap<>();
        synchronized (this) {
            while (downloadProcedures.size() < activeQueueSize && !waitingQueue.isEmpty()) {
                Map.Entry<String, SingleRunDownloadProcedure> nextTask = waitingQueue.pollFirst();
                downloadProcedures.put(nextTask.getKey(), nextTask.getValue());
                toStart.put(nextTask.getKey(), nextTask.getValue());
            }
        }

        toStart.forEach((operation, downloadProcedure) -> runDownload(operation, downloadProcedure));
    }

    private void runDownload(String operationId, SingleRunDownloadProcedure downloadProcedure) {
        String fingerprint = downloadProcedure.getRequest().fingerprint();
        String fileId = downloadProcedure.getRequest().fileId();
        Path destinationFilePath = downloadProcedure.getRequest().destinationFilePath();
        Path manifestFilePath = downloadProcedure.getRequest().manifestFilePath();
        Path temporaryFilePath = downloadProcedure.getRequest().temporaryFilePath();

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
                                            manifestFilePath.toAbsolutePath().toString(),
                                            DownloadFile.DownloadFileEntryStatus.DOWNLOADING,
                                            createInstantTime,
                                            createInstantTime
                                    );
                                }),
                        () -> fileDownloadStore.update(
                                operationId,
                                downloadFileEntry -> downloadFileEntry
                                        .withHash(downloadProcedure.getProgress().hash())
                                        .withTotal(downloadProcedure.getProgress().total())
                                        .withDownloaded(downloadProcedure.getProgress().downloaded())
                                        .withStatus(DownloadFile.DownloadFileEntryStatus.COMPLETED)
                                        .withUpdated(Instant.now())
                        )
                );
            } catch (Exception exception) {
                if (downloadProcedure.isStopped()) {
                    log.info("Download operation {} (fingerprint {}) was stopped by user request.",
                            operationId, fingerprint);
                    return;
                }

                log.error("Exception occurred during download process operation {} fingerprint {} {}",
                        operationId, fingerprint, exception.getMessage(), exception
                );
                fileDownloadStore.update(
                        operationId,
                        downloadFileEntry -> downloadFileEntry
                                .withHash(downloadProcedure.getProgress().hash())
                                .withTotal(downloadProcedure.getProgress().total())
                                .withDownloaded(downloadProcedure.getProgress().downloaded())
                                .withStatus(DownloadFile.DownloadFileEntryStatus.ERROR)
                                .withUpdated(Instant.now())
                );
                throw exception;
            } finally {
                synchronized (this) {
                    CommonUtils.executeSafety(() -> downloadProcedures.remove(operationId));
                }
                CommonUtils.executeSafety(() -> Files.deleteIfExists(temporaryFilePath));
                CommonUtils.executeSafety(() -> tryToStartNext());
            }
        });
    }

    public Map<String, DownloadProgress> getWaitingQueue() {
        List<Map.Entry<String, SingleRunDownloadProcedure>> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(waitingQueue);
        }

        return snapshot.stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        x -> x.getValue().getProgress(),
                        (o, o2) -> o2,
                        LinkedHashMap::new
                ));
    }

    public Map<String, DownloadProgress> getDownloadProcedures() {
        Map<String, SingleRunDownloadProcedure> snapshot;
        synchronized (this) {
            snapshot = new LinkedHashMap<>(downloadProcedures);
        }

        return snapshot.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        x -> x.getValue().getProgress(),
                        (o, o2) -> o2,
                        LinkedHashMap::new
                ));
    }

    public FileDownloadOrchestratorRemoveResponse rm(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        if (ObjectUtils.isEmpty(operationIdCriteriaEnvelopes)) {
            return new FileDownloadOrchestratorRemoveResponse(Map.of(), Map.of(), List.of(), Map.of());
        }

        Map<CriteriaEnvelope, String> active = new LinkedHashMap<>();
        CommonUtils.MatchResult<String> matchResult;

        synchronized (this) {
            Set<String> currentOperations = Stream.concat(
                    waitingQueue.stream().map(Map.Entry::getKey),
                    downloadProcedures.keySet().stream()
            ).collect(Collectors.toSet());

            matchResult = CommonUtils.matchBy(
                    currentOperations,
                    operationIdCriteriaEnvelopes,
                    (criteria, operationId) -> operationId.startsWith(criteria.value())
            );

            active.putAll(matchResult.found());
        }

        Collection<CriteriaEnvelope> canBeRemovedCriteria = operationIdCriteriaEnvelopes.stream()
                .filter(criteria -> !active.containsKey(criteria))
                .collect(Collectors.toSet());

        KeyValueStore.RemoveResult removeResult = fileDownloadStore.removeByCriteria(canBeRemovedCriteria);

        Map<CriteriaEnvelope, List<String>> ambiguous = Stream.concat(
                matchResult.ambiguous().entrySet().stream(),
                removeResult.ambiguous().entrySet().stream()
        ).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).distinct().toList(),
                LinkedHashMap::new
        ));

        return new FileDownloadOrchestratorRemoveResponse(
                removeResult.removed(),
                active,
                removeResult.notFound(),
                ambiguous
        );
    }

    public FileDownloadOrchestratorStopResponse stop(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        if (ObjectUtils.isEmpty(operationIdCriteriaEnvelopes)) {
            return new FileDownloadOrchestratorStopResponse(Map.of(), List.of(), Map.of());
        }

        Map<String, SingleRunDownloadProcedure> targetOperations = new LinkedHashMap<>();
        CommonUtils.MatchResult<String> matchResult;

        synchronized (this) {
            Set<String> currentOperations = Stream
                    .concat(
                            waitingQueue.stream().map(Map.Entry::getKey),
                            downloadProcedures.keySet().stream()
                    )
                    .collect(Collectors.toSet());

            matchResult = CommonUtils.matchBy(
                    currentOperations,
                    operationIdCriteriaEnvelopes,
                    (criteria, operationId) -> operationId.startsWith(criteria.value())
            );

            Collection<String> operationsToStop = Collections.unmodifiableCollection(matchResult.found().values());

            for (String operation : operationsToStop) {
                SingleRunDownloadProcedure procedure = downloadProcedures.get(operation);
                if (procedure != null) {
                    targetOperations.put(operation, procedure);
                }
            }

            Set<String> waitingToStop = new HashSet<>(operationsToStop);
            waitingToStop.removeAll(targetOperations.keySet());

            if (!waitingToStop.isEmpty()) {
                waitingQueue.removeIf(entry -> waitingToStop.contains(entry.getKey()));
            }
        }

        stop(targetOperations, Collections.emptyMap());

        return new FileDownloadOrchestratorStopResponse(
                matchResult.found(),
                matchResult.notFound(),
                matchResult.ambiguous()
        );
    }

    public FileDownloadOrchestratorKillResponse kill(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes) {
        if (ObjectUtils.isEmpty(operationIdCriteriaEnvelopes)) {
            return new FileDownloadOrchestratorKillResponse(Map.of(), List.of(), Map.of());
        }

        Map<String, SingleRunDownloadProcedure> targetOperations = new LinkedHashMap<>();
        CommonUtils.MatchResult<String> matchResult;

        synchronized (this) {
            Set<String> ramOperations = Stream.concat(
                    waitingQueue.stream().map(Map.Entry::getKey),
                    downloadProcedures.keySet().stream()
            ).collect(Collectors.toSet());

            Set<String> dbOperations = fileDownloadStore.getAll().keySet();

            Set<String> allOperations = Stream.concat(ramOperations.stream(), dbOperations.stream())
                    .collect(Collectors.toSet());

            matchResult = CommonUtils.matchBy(
                    allOperations,
                    operationIdCriteriaEnvelopes,
                    (criteria, operationId) -> operationId.startsWith(criteria.value())
            );

            Set<String> operationsToKill = new HashSet<>(matchResult.found().values());

            for (String operationId : operationsToKill) {
                SingleRunDownloadProcedure procedure = downloadProcedures.get(operationId);
                if (procedure != null) {
                    targetOperations.put(operationId, procedure);
                }
            }

            if (!operationsToKill.isEmpty()) {
                waitingQueue.removeIf(entry -> operationsToKill.contains(entry.getKey()));
            }
        }

        stop(targetOperations, Collections.emptyMap());

        fileDownloadStore.remove(matchResult.found().values());

        return new FileDownloadOrchestratorKillResponse(
                matchResult.found(),
                matchResult.notFound(),
                matchResult.ambiguous()
        );
    }

    public void killAll() {
        Map<String, SingleRunDownloadProcedure> targetOperations;

        synchronized (this) {
            targetOperations = new LinkedHashMap<>(downloadProcedures);
            waitingQueue.clear();
        }

        stop(targetOperations, Collections.emptyMap());

        fileDownloadStore.remove(fileDownloadStore.getAll().keySet());
    }

    public void stopAll() {
        Map<String, SingleRunDownloadProcedure> waitingQueueSnapshot;
        Map<String, SingleRunDownloadProcedure> proceduresSnapshot;

        synchronized (this) {
            waitingQueueSnapshot = waitingQueue.stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (o, o2) -> o2,
                    LinkedHashMap::new
            ));
            waitingQueue.clear();

            proceduresSnapshot = new LinkedHashMap<>(downloadProcedures);
            downloadProcedures.clear();
        }

        stop(proceduresSnapshot, waitingQueueSnapshot);
    }

    private void stop(Map<String, SingleRunDownloadProcedure> operations,
                      Map<String, SingleRunDownloadProcedure> waiting) {
        if (ObjectUtils.isEmpty(operations) && ObjectUtils.isEmpty(waiting)) {
            return;
        }

        operations.values().forEach(SingleRunDownloadProcedure::stop);

        fileDownloadStore.save(
                () -> {
                    Instant now = Instant.now();

                    return Stream
                            .concat(operations.entrySet().stream(), waiting.entrySet().stream())
                            .map(downloadProcedureEntry -> {
                                String operationId = downloadProcedureEntry.getKey();
                                SingleRunDownloadProcedure downloadProcedure = downloadProcedureEntry.getValue();

                                DownloadFile downloadFile = fileDownloadStore.get(operationId)
                                        .map(Map.Entry::getValue)
                                        .orElse(null);

                                if (downloadFile != null) {
                                    DownloadFile updated = downloadFile
                                            .withStatus(DownloadFile.DownloadFileEntryStatus.STOPPED)
                                            .withUpdated(now)
                                            .withHash(downloadProcedure.getProgress().hash())
                                            .withDownloaded(downloadProcedure.getProgress().downloaded())
                                            .withTotal(downloadProcedure.getProgress().total());
                                    return Map.entry(operationId, updated);
                                }

                                DownloadFile newOne = new DownloadFile(
                                        downloadProcedure.getRequest().fingerprint(),
                                        downloadProcedure.getRequest().fileId(),
                                        downloadProcedure.getRequest().destinationFilePath().toAbsolutePath().toString(),
                                        downloadProcedure.getRequest().temporaryFilePath().toAbsolutePath().toString(),
                                        downloadProcedure.getRequest().manifestFilePath().toAbsolutePath().toString(),
                                        DownloadFile.DownloadFileEntryStatus.STOPPED,
                                        now,
                                        now
                                );
                                return Map.entry(operationId, newOne);
                            })
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (v1, v2) -> v2,
                                    LinkedHashMap::new
                            ));
                },
                KeyValueStore.ValidatePolicy.GENTLE
        );
    }

    private Path getManifestFilePath(Path destinationFilePath) throws FileAlreadyExistsException {
        Path downloadDirectoryPath = daemonDownloadsDirectoryProvider.getDirectoryPath();

        Path manifestPath = downloadDirectoryPath.resolve(String.format("%s%s%s", "manifest.", destinationFilePath.getFileName().toString(), ".json"));

        if (Files.exists(manifestPath)) {
            throw new FileAlreadyExistsException("File already exists: %s".formatted(manifestPath));
        }

        return manifestPath;
    }

    private Path getDestinationFilePath(FileDownloadRequest request) throws FileAlreadyExistsException {
        if (!StringUtils.hasText(request.filename())) {
            throw new IllegalArgumentException("File download request failed. The request has am empty filename argument");
        }
        if (Paths.get(request.filename()).isAbsolute()) {
            throw new IllegalArgumentException("File download request failed. Absolute paths are not supported yet: " + request.filename());
        }

        Path downloadDirectoryPath = daemonDownloadsDirectoryProvider.getDirectoryPath();
        Path downloadFilePath = downloadDirectoryPath.resolve(request.filename());

        Stream.concat(
                        waitingQueue.stream().map(e -> Map.entry(e.getKey(), e.getValue().getProgress())),
                        downloadProcedures.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().getProgress()))
                )
                .filter(entry -> entry.getValue().filename().equals(downloadFilePath.toAbsolutePath().toString()))
                .findAny()
                .ifPresent(duplicate -> {
                    throw new IllegalStateException("File download request is already running operation %s file %s".formatted(
                            duplicate.getKey(), duplicate.getValue().filename()
                    ));
                });

        if (Files.exists(downloadFilePath)) {
            throw new FileAlreadyExistsException("File download request failed. File already exists: %s".formatted(downloadFilePath));
        }

        return downloadFilePath;
    }

    private Path getTemporaryFilePath(FileDownloadRequest request) {
        String temporaryFileName = CommonFileUtils.getTemporaryFileName(request.filename());
        Path downloadDirectoryPath = daemonDownloadsDirectoryProvider.getDirectoryPath();
        return downloadDirectoryPath.resolve(temporaryFileName);
    }

    @EventListener(ContextClosedEvent.class)
    public void contextClosedEventListener() throws InterruptedException {
        boolean set = closed.compareAndSet(false, true);
        if (!set) {
            return;
        }

        log.info("Closing {} by {}", FileDownloadOrchestrator.class, ContextClosedEvent.class);

        log.info("Stop All download procedures");
        stopAll();
        log.info("Stop All download procedures completed");

        log.info("Shutdown main executor service");
        fileDownloadingExecutorService.shutdown();
        log.info("Shutdown main executor service completed");
        log.info("AwaitTermination main executor service");
        boolean finishedCleanly = fileDownloadingExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        log.info("AwaitTermination main executor service completed. Result {}", finishedCleanly);
        if (!finishedCleanly) {
            log.info("ShutdownNow main executor service");
            fileDownloadingExecutorService.shutdownNow();
            log.info("ShutdownNow main executor service completed");
        }
        fileDownloadingExecutorService.close();
        log.info("Closed");
    }

    private void checkIfClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Already closed " + FileDownloadOrchestrator.class);
        }
    }

    public void rmAll() {
        Set<String> activeOperationIds;

        synchronized (this) {
            activeOperationIds = new HashSet<>(downloadProcedures.keySet());
        }

        Set<String> idsToRemove = fileDownloadStore.getAll().keySet().stream()
                .filter(s -> !activeOperationIds.contains(s))
                .collect(Collectors.toSet());

        fileDownloadStore.remove(idsToRemove);
    }

    // TODO add ETA
    public record DownloadProgress(String operationId,
                                   String fingerprint,
                                   String fileId,
                                   String filename,
                                   String hash,
                                   long total,
                                   long downloaded,
                                   long speedBytesPerSec,
                                   String percentage) {

    }

    public record FileDownloadOrchestratorRemoveResponse(
            Map<CriteriaEnvelope, String> removed,
            Map<CriteriaEnvelope, String> active,
            Collection<CriteriaEnvelope> notFound,
            Map<CriteriaEnvelope, List<String>> ambiguous
    ) {
    }

    public record FileDownloadOrchestratorStopResponse(
            Map<CriteriaEnvelope, String> found,
            Collection<CriteriaEnvelope> notFound,
            Map<CriteriaEnvelope, List<String>> ambiguous
    ) {
    }

    public record FileDownloadOrchestratorKillResponse(
            Map<CriteriaEnvelope, String> found,
            Collection<CriteriaEnvelope> notFound,
            Map<CriteriaEnvelope, List<String>> ambiguous
    ) {
    }
}
