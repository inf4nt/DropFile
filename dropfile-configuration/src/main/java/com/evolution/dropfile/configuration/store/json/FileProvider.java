package com.evolution.dropfile.configuration.store.json;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public interface FileProvider {

    Optional<String> getDirectoryName();

    String getFilename();

    @SneakyThrows
    default File getFile() {
        Path homePath = resolveHomePath();
        if (getDirectoryName().isPresent()) {
            homePath = homePath.resolve(getDirectoryName().get());
        }
        String filename = getFilename();
        Path configFilePath = homePath.resolve(filename);

        if (Files.notExists(configFilePath)) {
            Path parent = configFilePath.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(configFilePath);
        }
        return configFilePath.toFile();
    }

    default String getHomeDirectoryName() {
        return ".dropfile";
    }

    default Path resolveHomePath() {
        String homeDirectoryName = getHomeDirectoryName();
        Path userhomePath = Paths.get(System.getProperty("user.home"));
        return userhomePath
                .resolve(homeDirectoryName);
    }
}
