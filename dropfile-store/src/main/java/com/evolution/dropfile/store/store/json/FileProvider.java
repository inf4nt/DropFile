package com.evolution.dropfile.store.store.json;

import com.evolution.dropfile.common.CommonFileUtils;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface FileProvider {

    String getFileName();

    default Path getFilePath() {
        Path homePath = resolveHomePath();
        return homePath.resolve(getFileName());
    }

    default Path getTempFilePath() {
        Path homePath = resolveHomePath();
        String fileName = getFileName();
        String tmpFileName = CommonFileUtils.getTemporaryFileName(fileName);
        return homePath.resolve(tmpFileName);
    }

    @SneakyThrows
    default File getOrCreateTempFile() {
        Path path = getTempFilePath();

        if (Files.notExists(path)) {
            Path parent = path.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(path);
        }

        return path.toFile();
    }

    @SneakyThrows
    default File getOrCreateFile() {
        Path configFilePath = getFilePath();

        if (Files.notExists(configFilePath)) {
            Path parent = configFilePath.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(configFilePath);
        }

        return configFilePath.toFile();
    }

    default Path resolveHomePath() {
        return Paths.get(System.getProperty("user.home"), ".dropfile");
    }
}
