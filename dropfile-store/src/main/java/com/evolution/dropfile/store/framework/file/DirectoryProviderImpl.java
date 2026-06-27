package com.evolution.dropfile.store.framework.file;

import java.nio.file.Path;

public class DirectoryProviderImpl implements DirectoryProvider {

    private final Path directoryPath;

    public DirectoryProviderImpl(Path root) {
        if (!root.isAbsolute()) {
            throw new IllegalArgumentException("Root path must be absolute. Got relative path: " + root);
        }
        this.directoryPath = root;
    }

    public DirectoryProviderImpl(DirectoryProvider directoryProvider, Path directoryRelativePath) {
        if (directoryRelativePath.isAbsolute()) {
            throw new IllegalArgumentException("Path must be relative. Got absolute path: " + directoryRelativePath);
        }
        this.directoryPath = directoryProvider.getDirectoryPath().resolve(directoryRelativePath);
    }

    @Override
    public Path getDirectoryPath() {
        return directoryPath;
    }
}
