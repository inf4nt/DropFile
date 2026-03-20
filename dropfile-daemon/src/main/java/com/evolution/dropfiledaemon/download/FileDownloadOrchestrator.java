package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.download.procedure.DownloadProcedure;
import com.evolution.dropfiledaemon.download.procedure.DownloadProcedureFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
@Component
public class FileDownloadOrchestrator implements AutoCloseable {

    private final ExecutorService fileDownloadingExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, DownloadProcedure> downloadProcedures = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Deque<Map.Entry<String, DownloadProcedure>> waitingQueue = new ArrayDeque<>();

    private final DownloadProcedureFactory downloadProcedureFactory;

    private final ApplicationConfigStore applicationConfigStore;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private int getAvailablePermits() {
        int total = downloadProcedures.size() + waitingQueue.size();
        return daemonApplicationProperties.downloadOrchestratorMaxQueueSize - total;
    }

    public synchronized FileDownloadResponseEnvelope start(List<FileDownloadRequest> requests, boolean force) {
        int availablePermits = getAvailablePermits();
        if (availablePermits - requests.size() < 0 && !force) {
            throw new IllegalStateException(String.format("No available permits. Total: %s Requests %s", availablePermits, requests.size()));
        }

        List<FileDownloadResponse> responses = new ArrayList<>();
        List<FileDownloadRequest> skipped = new ArrayList<>();
        for (FileDownloadRequest request : requests) {
            try {
                FileDownloadResponse response = start(request);
                responses.add(response);
            } catch (Exception e) {
                if (!force) {
                    throw e;
                }
                skipped.add(request);
            }
        }

        return new FileDownloadResponseEnvelope(
                Collections.unmodifiableList(responses),
                Collections.unmodifiableList(skipped)
        );
    }

    @SneakyThrows
    private FileDownloadResponse start(FileDownloadRequest request) {
        int downloadOrchestratorMaxQueueSize = daemonApplicationProperties.downloadOrchestratorMaxQueueSize;
        if (downloadProcedures.size() + waitingQueue.size() >= downloadOrchestratorMaxQueueSize) {
            throw new IllegalStateException("No available permits. Total: " + downloadOrchestratorMaxQueueSize);
        }

        String operationId = CommonUtils.random();
        Path destinationFilePath = getDestinationFilePath(request);
        Path temporaryFilePath = getTemporaryFilePath(request);
        DownloadProcedure downloadProcedure = downloadProcedureFactory.get(
                operationId,
                request.fingerprint(),
                request.fileId(),
                request.filename(),
                destinationFilePath,
                temporaryFilePath
        );
        waitingQueue.add(new AbstractMap.SimpleEntry<>(operationId, downloadProcedure));

        tryToStartNext();

        return new FileDownloadResponse(operationId, request.fingerprint(), request.fileId(), destinationFilePath.toAbsolutePath().toString());
    }

    private synchronized void tryToStartNext() {
        int activeQueueSize = daemonApplicationProperties.downloadOrchestratorActiveQueueSize;

        while (downloadProcedures.size() < activeQueueSize && !waitingQueue.isEmpty()) {
            Map.Entry<String, DownloadProcedure> nextTask = waitingQueue.pollFirst();
            downloadProcedures.put(nextTask.getKey(), nextTask.getValue());

            runDownload(nextTask.getKey(), nextTask.getValue());
        }
    }

    private void runDownload(String operationId, DownloadProcedure downloadProcedure) {
        String fingerprint = downloadProcedure.getProgress().fingerprint();
        String fileId = downloadProcedure.getProgress().fileId();
        Path destinationFilePath = downloadProcedure.getRequest().destinationFilePath();
        Path temporaryFilePath = downloadProcedure.getRequest().temporaryFilePath();

        fileDownloadingExecutorService.execute(() -> {
            try {
                downloadProcedure.run(
                        () -> applicationConfigStore.getFileDownloadEntryStore().save(
                                operationId,
                                new DownloadFileEntry(
                                        fingerprint,
                                        fileId,
                                        destinationFilePath.toAbsolutePath().toString(),
                                        temporaryFilePath.toAbsolutePath().toString(),
                                        DownloadFileEntry.DownloadFileEntryStatus.DOWNLOADING,
                                        Instant.now(),
                                        Instant.now()
                                )
                        ),
                        () -> applicationConfigStore.getFileDownloadEntryStore()
                                .update(
                                        operationId,
                                        downloadFileEntry -> downloadFileEntry
                                                .withHash(downloadProcedure.getProgress().hash())
                                                .withTotal(downloadProcedure.getProgress().total())
                                                .withDownloaded(downloadProcedure.getProgress().downloaded())
                                                .withStatus(DownloadFileEntry.DownloadFileEntryStatus.COMPLETED)
                                                .withUpdated(Instant.now())
                                )
                );
            } catch (Exception exception) {
                log.info("Exception occurred during download process operation {} fingerprint {} {}",
                        operationId, fingerprint, exception.getMessage(), exception
                );
                CommonUtils.executeSafety(() -> {
                    if (downloadProcedure.isStopped()) {
                        return;
                    }
                    applicationConfigStore.getFileDownloadEntryStore()
                            .update(
                                    operationId,
                                    downloadFileEntry -> downloadFileEntry
                                            .withHash(downloadProcedure.getProgress().hash())
                                            .withTotal(downloadProcedure.getProgress().total())
                                            .withDownloaded(downloadProcedure.getProgress().downloaded())
                                            .withStatus(DownloadFileEntry.DownloadFileEntryStatus.ERROR)
                                            .withUpdated(Instant.now())
                            );
                });
                throw new RuntimeException(exception);
            } finally {
                CommonUtils.executeSafety(() -> downloadProcedures.remove(operationId));
                CommonUtils.executeSafety(() -> Files.deleteIfExists(temporaryFilePath));
                CommonUtils.executeSafety(() -> tryToStartNext());
            }
        });
    }

    public Map<String, DownloadProgress> getDownloadProcedures() {
        return downloadProcedures.entrySet().stream()
                .collect(Collectors.toMap(
                        it -> it.getKey(),
                        it -> it.getValue().getProgress()
                ));
    }

    public void stop(String operation) {
        DownloadProcedure downloadProcedure = downloadProcedures.get(operation);
        if (downloadProcedure == null) {
            throw new RuntimeException("No operation found: " + operation);
        }
        downloadProcedures.remove(operation);
        stop(Map.of(operation, downloadProcedure), Collections.emptyMap());
    }

    public void stopAll() {
        Map<String, DownloadProcedure> waitingQueueSnapshot = new LinkedHashMap<>();
        while (!waitingQueue.isEmpty()) {
            Map.Entry<String, DownloadProcedure> entry = waitingQueue.pollFirst();
            waitingQueueSnapshot.put(entry.getKey(), entry.getValue());
        }
        Map<String, DownloadProcedure> proceduresSnapshot = new LinkedHashMap<>(downloadProcedures);
        proceduresSnapshot.entrySet().forEach(key -> downloadProcedures.remove(key));
        stop(proceduresSnapshot, waitingQueueSnapshot);
    }

    private void stop(Map<String, DownloadProcedure> operations,
                      Map<String, DownloadProcedure> waiting) {
        operations.values().forEach(it -> it.stop());

        FileDownloadEntryStore fileDownloadEntryStore = applicationConfigStore.getFileDownloadEntryStore();
        fileDownloadEntryStore.save(
                () -> Stream
                        .concat(operations.entrySet().stream(), waiting.entrySet().stream())
                        .map(downloadProcedureEntry -> {
                            String operationId = downloadProcedureEntry.getKey();

                            DownloadFileEntry downloadFileEntry = fileDownloadEntryStore.get(operationId)
                                    .map(it -> it.getValue())
                                    .orElse(null);

                            DownloadProcedure downloadProcedure = downloadProcedureEntry.getValue();

                            if (downloadFileEntry != null) {
                                DownloadFileEntry updated = downloadFileEntry
                                        .withStatus(DownloadFileEntry.DownloadFileEntryStatus.STOPPED)
                                        .withUpdated(Instant.now())
                                        .withHash(downloadProcedure.getProgress().hash())
                                        .withDownloaded(downloadProcedure.getProgress().downloaded())
                                        .withTotal(downloadProcedure.getProgress().total());
                                return new AbstractMap.SimpleEntry<>(operationId, updated);
                            }
                            DownloadFileEntry newOne = new DownloadFileEntry(
                                    downloadProcedure.getRequest().fingerprint(),
                                    downloadProcedure.getProgress().fileId(),
                                    downloadProcedure.getRequest().destinationFilePath().toAbsolutePath().toString(),
                                    downloadProcedure.getRequest().temporaryFilePath().toAbsolutePath().toString(),
                                    DownloadFileEntry.DownloadFileEntryStatus.STOPPED,
                                    Instant.now(),
                                    Instant.now()
                            );
                            return new AbstractMap.SimpleEntry<>(operationId, newOne);
                        })
                        .collect(Collectors.toMap(
                                x -> x.getKey(),
                                x -> x.getValue(),
                                (v1, v2) -> v2,
                                () -> new LinkedHashMap<>()
                        )),
                KeyValueStore.ValidatePolicy.GENTLE
        );
    }

    private Path getDestinationFilePath(FileDownloadRequest request) {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }

        if (Paths.get(request.filename()).isAbsolute()) {
            throw new UnsupportedOperationException("Absolute paths are not supported yet: " + request.filename());
        }

        String downloadDirectory = daemonApplicationProperties.downloadDirectory;
        Path downloadFilePath = Paths.get(downloadDirectory, request.filename()).toAbsolutePath();

        if (Files.exists(downloadFilePath)) {
            throw new IllegalArgumentException(String.format("file already exists: %s", downloadFilePath));
        }

        return downloadFilePath;
    }

    private Path getTemporaryFilePath(FileDownloadRequest request) {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }
        if (Paths.get(request.filename()).isAbsolute()) {
            throw new UnsupportedOperationException("filename must not be absolute. Unsupported yet: " + request.filename());
        }

        String temporaryFileName = CommonFileUtils.getTemporaryFileName(request.filename());
        String downloadDirectory = daemonApplicationProperties.downloadDirectory;
        return Paths.get(downloadDirectory, temporaryFileName).toAbsolutePath();
    }

    @SneakyThrows
    @Override
    public void close() {
        log.info("Closing FileDownloadOrchestrator");
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
}
