package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.download.DownloadFile;
import com.evolution.dropfile.store.download.FileDownloadStore;
import com.evolution.dropfiledaemon.bootstrap.ApplicationInitializationPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Order(4)
@RequiredArgsConstructor
@Component
public class CleanupFileDownloadApplicationInitializationPhase
        implements ApplicationInitializationPhase {

    private final FileDownloadStore fileDownloadStore;

    @Override
    public void execute() throws Exception {
        List<DownloadFile> candidates = fileDownloadStore.getAll()
                .values()
                .stream()
                .filter(this::isCleanupCandidate)
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Startup disk cleanup: found {} candidate entries for resource deletion", candidates.size());

        int count = 0;
        try {
            for (DownloadFile downloadFile : candidates) {
                if (cleanupFilesOnDisk(downloadFile)) {
                    count++;
                }
            }
        } finally {
            log.info("Startup disk cleanup: removed elements {}", count);
        }
    }

    private boolean isCleanupCandidate(DownloadFile downloadFile) {
        return downloadFile.status() != DownloadFile.DownloadFileEntryStatus.COMPLETED;
    }

    private boolean cleanupFilesOnDisk(DownloadFile downloadFile) {
        Path tempPath = Paths.get(downloadFile.temporaryFile());
        try {
            if (Files.deleteIfExists(tempPath)) {
                log.info("Startup disk cleanup: deleted temp file {}", tempPath);
                return true;
            }
        } catch (IOException e) {
            log.error("Startup disk cleanup: failed to delete temp file {} - {}", tempPath, e.getMessage(), e);
        }
        return false;
    }
}
