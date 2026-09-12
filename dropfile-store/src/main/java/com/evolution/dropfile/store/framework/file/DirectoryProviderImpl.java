package com.evolution.dropfile.store.framework.file;

import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryProviderImpl implements DirectoryProvider {

    private final Path directoryPath;

    public DirectoryProviderImpl(Path root) {
        if (!root.isAbsolute()) {
            throw new IllegalArgumentException("Root path must be absolute. Got relative path: " + root);
        }
        if (Files.exists(root)) {
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException("Root path must resolve to a directory: " + root);
            }
        }
        this.directoryPath = root;
    }

    public DirectoryProviderImpl(DirectoryProvider directoryProvider, Path directoryRelativePath) {
        if (directoryRelativePath.isAbsolute()) {
            throw new IllegalArgumentException("Path must be relative. Got absolute path: " + directoryRelativePath);
        }
        Path resolvedPath = directoryProvider.getDirectoryPath().resolve(directoryRelativePath);

        if (Files.exists(resolvedPath)) {
            if (!Files.isDirectory(resolvedPath)) {
                throw new IllegalArgumentException("Target path must resolve to a directory: " + resolvedPath);
            }
        }

        this.directoryPath = resolvedPath;
    }

    @Override
    public Path getDirectoryPath() {
        return directoryPath;
    }
}
