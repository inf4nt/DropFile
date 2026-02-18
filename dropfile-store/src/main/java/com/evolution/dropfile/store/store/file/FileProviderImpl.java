package com.evolution.dropfile.store.store.file;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class FileProviderImpl implements FileProvider {

    private final String filename;

    public FileProviderImpl(String filename) {
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
        if (isWindows()) {
            String basePath = System.getenv("LOCALAPPDATA");
            Objects.requireNonNull(basePath);
            return Path.of(basePath, "DropFile");
        }
        if (isLinux() || isMacOs()) {
            return Paths.get(System.getProperty("user.home"), ".dropfile");
        }
        throw new UnsupportedOperationException(
                "Unsupported operating system " + getOs()
        );
    }

    @Override
    public Path getFilePath() {
        String filename = getFilename();
        return getHomePath().resolve(filename);
    }

    private boolean isMacOs() {
        return getOs().toLowerCase().startsWith("mac");
    }

    private boolean isLinux() {
        return getOs().toLowerCase().startsWith("linux");
    }

    private boolean isWindows() {
        return getOs().toLowerCase().startsWith("windows");
    }

    private String getOs() {
        return System.getProperty("os.name");
    }
}
