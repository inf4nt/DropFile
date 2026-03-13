package com.evolution.dropfile.store.app.daemon;

import com.evolution.dropfile.store.framework.single.StoreInitializationProcedure;
import lombok.SneakyThrows;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DaemonAppConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<DaemonAppConfigStore> {

    @SneakyThrows
    @Override
    public void init(DaemonAppConfigStore store) {
        DaemonAppConfig daemonAppConfig = store.get().orElse(null);

        if (daemonAppConfig != null) {
            String downloadDirectory = daemonAppConfig.downloadDirectory();
            validateDaemonDownloadDirectory(downloadDirectory);
            return;
        }

        Path homeDir = Paths.get(System.getProperty("user.home"), "/DropfileDownloads");
        if (Files.notExists(homeDir)) {
            Files.createDirectories(homeDir);
        }

        int daemonPort = 18181;
        int downloadOrchestratorThreadSize = 10;
        int downloadProcedureThreadSize = 2;
        int downloadProcedureManifestCallTimeoutMillis = 300_000;
        int downloadProcedureChunkCallTimeoutMillis = 120_000;

        boolean compressTunnelActive = true;
        int compressTunnelLevel = 3;

        store.save(
                new DaemonAppConfig(
                        homeDir.toAbsolutePath().toString(),
                        daemonPort,
                        downloadOrchestratorThreadSize,
                        downloadProcedureThreadSize,
                        downloadProcedureManifestCallTimeoutMillis,
                        downloadProcedureChunkCallTimeoutMillis,
                        compressTunnelActive,
                        compressTunnelLevel
                )
        );
    }

    @SneakyThrows
    private void validateDaemonDownloadDirectory(String daemonDownloadDirectory) {
        if (daemonDownloadDirectory == null) {
            throw new IllegalArgumentException("Daemon download directory is null");
        }
        if (daemonDownloadDirectory.isBlank()) {
            throw new IllegalArgumentException("Daemon download directory is empty or blank string");
        }
        Path daemonDownloadDirectoryPath = Paths.get(daemonDownloadDirectory)
                .normalize();
        if (!daemonDownloadDirectoryPath.isAbsolute()) {
            throw new IllegalArgumentException("Daemon download directory has to be absolute path: " + daemonDownloadDirectoryPath);
        }
        if (Files.notExists(daemonDownloadDirectoryPath)) {
            throw new FileNotFoundException("Daemon download directory does not exist: " + daemonDownloadDirectoryPath);
        }
    }
}
