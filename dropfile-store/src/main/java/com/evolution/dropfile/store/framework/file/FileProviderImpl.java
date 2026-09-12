package com.evolution.dropfile.store.framework.file;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class FileProviderImpl implements FileProvider {

    private final Path filePath;

    public FileProviderImpl(DirectoryProvider directoryProvider, Path relativeFilePath) {
        if (relativeFilePath.isAbsolute()) {
            throw new IllegalArgumentException("File path must be relative. Got absolute path: " + relativeFilePath);
        }

        Path resolvedPath = directoryProvider.getDirectoryPath().resolve(relativeFilePath);

        if (Files.exists(resolvedPath)) {
            if (!Files.isRegularFile(resolvedPath, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(resolvedPath)) {
                throw new IllegalArgumentException("Target path must be a regular file and not a symbolic link: " + resolvedPath);
            }
        }

        this.filePath = resolvedPath;
    }

    @Override
    public Path getFilePath() {
        return filePath;
    }
}
