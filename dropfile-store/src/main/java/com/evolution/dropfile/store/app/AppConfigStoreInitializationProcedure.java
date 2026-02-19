package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.store.single.StoreInitializationProcedure;
import lombok.SneakyThrows;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class AppConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<AppConfigStore> {

    @SneakyThrows
    @Override
    public void init(AppConfigStore store) {
        store.init();

        Optional<AppConfig> configOptional = store.get();
        if (configOptional.isPresent()) {
            String downloadDirectory = configOptional.get().daemonAppConfig().downloadDirectory();
            validateDaemonDownloadDirectory(downloadDirectory);
            Path daemonDownloadDirectoryPath = Paths.get(downloadDirectory);
            if (Files.notExists(daemonDownloadDirectoryPath)) {
                Files.createDirectories(daemonDownloadDirectoryPath);
            }
            return;
        }

        Path homeDir = Paths.get(System.getProperty("user.home"), "/DropfileDownloads");
        if (Files.notExists(homeDir)) {
            Files.createDirectories(homeDir);
        }

        Integer daemonPort = 18181;
        AppConfig config = new AppConfig(
                new AppConfig.CliAppConfig(
                        "127.0.0.1",
                        daemonPort
                ),
                new AppConfig.DaemonAppConfig(
                        homeDir.toAbsolutePath().toString(),
                        daemonPort
                )
        );
        store.save(config);
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
