package com.evolution.dropfile.store.framework.file;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileProviderImpl implements FileProvider {

    private final Path parrentDirectoryPath;

    private final String filename;

    public FileProviderImpl(Path parrentDirectoryPath, String filename) {
        this.parrentDirectoryPath = parrentDirectoryPath;
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @SneakyThrows
    @Override
    public File getOrCreateFile() {
        Path filePath = getFilePath();
        if (Files.notExists(filePath)) {
            Path parent = filePath.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.createFile(filePath);
        }
        return filePath.toFile();
    }

    @Override
    public Path getHomePath() {
        return parrentDirectoryPath;
    }

    @Override
    public Path getFilePath() {
        return getHomePath().resolve(filename);
    }
}
