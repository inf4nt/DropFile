package com.evolution.dropfile.store.framework.file;

import java.nio.file.Path;

public class FileProviderImpl implements FileProvider {

    private final Path filePath;

    public FileProviderImpl(DirectoryProvider directoryProvider, Path relativeFilePath) {
        if (relativeFilePath.isAbsolute()) {
            throw new IllegalArgumentException("File path must be relative. Got absolute path: " + relativeFilePath);
        }
        this.filePath = directoryProvider.getDirectoryPath().resolve(relativeFilePath);
    }

    @Override
    public Path getFilePath() {
        return filePath;
    }
}
