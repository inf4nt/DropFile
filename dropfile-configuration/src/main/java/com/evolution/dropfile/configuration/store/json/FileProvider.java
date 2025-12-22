package com.evolution.dropfile.configuration.store.json;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface FileProvider {

    Path getFilePath();

    @SneakyThrows
    default File getFile() {
        Path homePath = resolveHomePath();
        Path configFilePath = homePath.resolve(getFilePath());

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
