package com.evolution.dropfile.store.store.json;

import java.nio.file.Path;
import java.util.Objects;

@Deprecated
public interface FileProtectedProvider extends FileProvider {

    @Override
    default Path resolveHomePath() {
        if (isWindows()) {
            String basePath = System.getenv("LOCALAPPDATA");
            Objects.requireNonNull(basePath);
            return Path.of(basePath, "DropFile");
        }
        if (isLinux() || isMacOs()) {
            return FileProvider.super.resolveHomePath();
        }
        throw new UnsupportedOperationException(
                "Unsupported operating system " + getOs()
        );
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
