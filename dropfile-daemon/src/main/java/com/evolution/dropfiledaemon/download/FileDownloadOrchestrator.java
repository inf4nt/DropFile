package com.evolution.dropfiledaemon.download;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.download.DownloadFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.download.exception.DownloadingStoppedException;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.util.FileHelper;
import com.evolution.dropfiledaemon.util.SafeUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadOrchestrator {

    private static final int MAX_PARALLEL_DOWNLOADING_COUNT = 10;

    private static final int MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT = 2;

    private final Semaphore downloadingSemaphore = new Semaphore(MAX_PARALLEL_DOWNLOADING_COUNT);

    private final ExecutorService fileDownloadingExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, DownloadProcedure> downloadProcedures = Collections.synchronizedMap(new LinkedHashMap<>());

    private final TunnelClient tunnelClient;

    private final ApplicationConfigStore applicationConfigStore;

    private final FileHelper fileHelper;

    @SneakyThrows
    public FileDownloadResponse start(FileDownloadRequest request) {
        if (downloadingSemaphore.availablePermits() == 0) {
            throw new IllegalStateException("No available permits. Total: " + MAX_PARALLEL_DOWNLOADING_COUNT);
        }

        downloadingSemaphore.acquire();
        File destinationFile;
        File temporaryFile;
        try {
            destinationFile = getDestinationFile(request);
            temporaryFile = getTemporaryFile(request);
        } catch (Exception exception) {
            SafeUtils.execute(() -> downloadingSemaphore.release());
            log.info("Exception occurred during getting file {}", exception.getMessage(), exception);
            throw exception;
        }

        String operationId = CommonUtils.random();
        ExecutorService downloadProcedureExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        DownloadProcedure downloadProcedure = new DownloadProcedure(
                new Semaphore(MAX_PARALLEL_DOWNLOADING_CHUNK_COUNT),
                downloadProcedureExecutorService,
                tunnelClient,
                fileHelper,
                operationId,
                request,
                destinationFile,
                temporaryFile
        );
        downloadProcedures.put(operationId, downloadProcedure);
        applicationConfigStore.getFileDownloadEntryStore().save(
                operationId,
                new DownloadFileEntry(
                        request.fingerprintConnection(),
                        request.fileId(),
                        destinationFile.getAbsolutePath(),
                        temporaryFile.getAbsolutePath(),
                        null,
                        0,
                        0,
                        DownloadFileEntry.DownloadFileEntryStatus.DOWNLOADING,
                        Instant.now()
                )
        );
        fileDownloadingExecutorService.execute(() -> {
            try {
                downloadProcedure.run();
                applicationConfigStore.getFileDownloadEntryStore().save(
                        operationId,
                        new DownloadFileEntry(
                                request.fingerprintConnection(),
                                request.fileId(),
                                destinationFile.getAbsolutePath(),
                                temporaryFile.getAbsolutePath(),
                                downloadProcedure.getProgress().hash(),
                                downloadProcedure.getProgress().downloaded(),
                                downloadProcedure.getProgress().total(),
                                DownloadFileEntry.DownloadFileEntryStatus.COMPLETED,
                                Instant.now()
                        )
                );
            } catch (Exception exception) {
                DownloadFileEntry.DownloadFileEntryStatus status = isStopped(exception) ?
                        DownloadFileEntry.DownloadFileEntryStatus.STOPPED :
                        DownloadFileEntry.DownloadFileEntryStatus.ERROR;

                applicationConfigStore.getFileDownloadEntryStore().save(
                        operationId,
                        new DownloadFileEntry(
                                request.fingerprintConnection(),
                                request.fileId(),
                                destinationFile.getAbsolutePath(),
                                temporaryFile.getAbsolutePath(),
                                downloadProcedure.getProgress().hash(),
                                downloadProcedure.getProgress().downloaded(),
                                downloadProcedure.getProgress().total(),
                                status,
                                Instant.now()
                        )
                );
                log.info("Exception occurred during download process operation {} fingerprint {} {}",
                        operationId, request.fingerprintConnection(), exception.getMessage(), exception);
                throw new RuntimeException(exception);
            } finally {
                SafeUtils.execute(() -> downloadProcedures.remove(operationId));
                SafeUtils.execute(() -> downloadProcedureExecutorService.close());
                SafeUtils.execute(() -> downloadingSemaphore.release());
            }
        });
        return new FileDownloadResponse(operationId, destinationFile.getAbsolutePath());
    }

    private boolean isStopped(Exception exception) {
        return exception instanceof DownloadingStoppedException || exception.getCause() instanceof DownloadingStoppedException;
    }

    public Map<String, DownloadProgress> getDownloadProcedures() {
        Map<String, DownloadProgress> result = new LinkedHashMap<>();
        for (Map.Entry<String, DownloadProcedure> entry : downloadProcedures.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getProgress());
        }
        return result;
    }

    public void stopAll() {
        for (DownloadProcedure procedure : downloadProcedures.values()) {
            procedure.stop();
        }
    }

    public void stop(String operation) {
        DownloadProcedure downloadProcedure = downloadProcedures.get(operation);
        if (downloadProcedure == null) {
            throw new RuntimeException("No operation found: " + operation);
        }
        downloadProcedure.stop();
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

    public record DownloadProgress(String operationId,
                                   String fingerprint,
                                   String fileId,
                                   String filename,
                                   String hash,
                                   long downloaded,
                                   long total,
                                   String speed,
                                   String percentage) {

    }
}
