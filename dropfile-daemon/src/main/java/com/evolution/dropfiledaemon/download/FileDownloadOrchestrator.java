package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.util.SafeUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileDownloadOrchestrator implements AutoCloseable {

    private final ExecutorService fileDownloadingExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, DownloadProcedure> downloadProcedures = Collections.synchronizedMap(new LinkedHashMap<>());

    private final DownloadProcedureFactory downloadProcedureFactory;

    private final ApplicationConfigStore applicationConfigStore;

    private final int downloadOrchestratorThreadSize;

    @Autowired
    public FileDownloadOrchestrator(DownloadProcedureFactory downloadProcedureFactory,
                                    ApplicationConfigStore applicationConfigStore,
                                    @Value("${download.ochestrator.thread-size}") int downloadOrchestratorThreadSize) {
        this.downloadProcedureFactory = downloadProcedureFactory;
        this.applicationConfigStore = applicationConfigStore;
        this.downloadOrchestratorThreadSize = downloadOrchestratorThreadSize;
    }

    @SneakyThrows
    public synchronized FileDownloadResponse start(FileDownloadRequest request) {
        if (downloadProcedures.size() >= downloadOrchestratorThreadSize) {
            throw new IllegalStateException("No available permits. Total: " + downloadOrchestratorThreadSize);
        }

        String operationId = CommonUtils.random();
        File destinationFile = getDestinationFile(request);
        File temporaryFile = getTemporaryFile(request);
        DownloadProcedure downloadProcedure = downloadProcedureFactory.get(
                operationId,
                request.fingerprint(),
                request.fileId(),
                request.filename(),
                destinationFile,
                temporaryFile
        );
        downloadProcedures.put(operationId, downloadProcedure);

        fileDownloadingExecutorService.execute(() -> {
            try {
                applicationConfigStore.getFileDownloadEntryStore().save(
                        operationId,
                        new DownloadFileEntry(
                                request.fingerprint(),
                                request.fileId(),
                                destinationFile.getAbsolutePath(),
                                temporaryFile.getAbsolutePath(),
                                DownloadFileEntry.DownloadFileEntryStatus.DOWNLOADING,
                                Instant.now(),
                                Instant.now()
                        )
                );
                downloadProcedure.run();
                applicationConfigStore.getFileDownloadEntryStore()
                        .update(
                                operationId,
                                downloadFileEntry -> downloadFileEntry
                                        .withHash(downloadProcedure.getProgress().hash())
                                        .withTotal(downloadProcedure.getProgress().total())
                                        .withDownloaded(downloadProcedure.getProgress().downloaded())
                                        .withStatus(DownloadFileEntry.DownloadFileEntryStatus.COMPLETED)
                                        .withUpdated(Instant.now())
                        );
            } catch (Exception exception) {
                log.info("Exception occurred during download process operation {} fingerprint {} {}",
                        operationId, request.fingerprint(), exception.getMessage(), exception
                );
                SafeUtils.execute(() -> {
                    boolean stopped = isStopped(operationId);
                    DownloadFileEntry.DownloadFileEntryStatus status = stopped ?
                            DownloadFileEntry.DownloadFileEntryStatus.STOPPED : DownloadFileEntry.DownloadFileEntryStatus.ERROR;
                    applicationConfigStore.getFileDownloadEntryStore()
                            .update(
                                    operationId,
                                    downloadFileEntry -> downloadFileEntry
                                            .withHash(downloadProcedure.getProgress().hash())
                                            .withTotal(downloadProcedure.getProgress().total())
                                            .withDownloaded(downloadProcedure.getProgress().downloaded())
                                            .withStatus(status)
                                            .withUpdated(Instant.now())
                            );
                });
                throw new RuntimeException(exception);
            } finally {
                SafeUtils.execute(() -> downloadProcedures.remove(operationId));
                SafeUtils.execute(() -> Files.deleteIfExists(temporaryFile.toPath()));
            }
        });
        return new FileDownloadResponse(operationId, request.fileId(), destinationFile.getAbsolutePath());
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
        downloadProcedure.stop();
    }

    public void stopAll() {
        downloadProcedures.values().forEach(it -> it.stop());
    }

    private boolean isStopped(String operation) {
        return Optional.ofNullable(downloadProcedures.get(operation))
                .map(it -> it.isStopped())
                .orElse(false);
    }

    private File getDestinationFile(FileDownloadRequest request) throws IOException {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }

        if (Paths.get(request.filename()).isAbsolute()) {
            throw new UnsupportedOperationException("Absolute paths are not supported yet: " + request.filename());
        }

        String downloadDirectory = applicationConfigStore.getAppConfigStore().getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile = new File(downloadDirectory, request.filename()).getCanonicalFile();

        if (Files.exists(downloadFile.toPath())) {
            throw new IllegalArgumentException(String.format("file already exists: %s", downloadFile.getAbsolutePath()));
        }

        return downloadFile;
    }

    private File getTemporaryFile(FileDownloadRequest request) throws IOException {
        if (ObjectUtils.isEmpty(request.filename())) {
            throw new IllegalArgumentException("filename must not be empty");
        }
        if (Paths.get(request.filename()).isAbsolute()) {
            throw new UnsupportedOperationException("filename must not be absolute. Unsupported yet: " + request.filename());
        }

        String temporaryFileName = CommonFileUtils.getTemporaryFileName(request.filename());
        String downloadDirectory = applicationConfigStore.getAppConfigStore().getRequired().daemonAppConfig().downloadDirectory();
        File tmpDownloadFile = new File(downloadDirectory, temporaryFileName).getCanonicalFile();

        if (Files.notExists(tmpDownloadFile.toPath())) {
            Files.createFile(tmpDownloadFile.toPath());
        }

        return tmpDownloadFile;
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
