package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.store.single.StoreInitializationProcedure;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class AppConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<AppConfigStore> {

    @SneakyThrows
    @Override
    public void init(AppConfigStore store) {
        Optional<AppConfig> configOptional = store.get();
        if (configOptional.isPresent()) {
            String downloadDirectory = configOptional.get().daemonAppConfig().downloadDirectory();
            Path downloadDirectoryPath = Paths.get(downloadDirectory);
            if (Files.notExists(downloadDirectoryPath)) {
                Files.createDirectories(downloadDirectoryPath);
            }
            return;
        }

        Path homeDir = Paths.get(System.getProperty("user.home"), ".dropfile");
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
}
