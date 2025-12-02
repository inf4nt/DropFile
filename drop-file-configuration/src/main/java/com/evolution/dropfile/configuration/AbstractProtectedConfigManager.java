package com.evolution.dropfile.configuration;

import java.nio.file.Path;

public abstract class AbstractProtectedConfigManager {

    private static final String WINDOWS_HOME_DIR = "DropFile";

    private static final String UNIX_HOME_DIR = ".dropfile";

    protected Path resolveHomePath() {
        String basePath;
        String dirName;
        if (isWindows()) {
            basePath = System.getenv("LOCALAPPDATA");
            dirName = WINDOWS_HOME_DIR;
        } else if (isLinux() || isMacOs()) {
            basePath = System.getProperty("user.home");
            dirName = UNIX_HOME_DIR;
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported operating system " + getOs()
            );
        }
        return Path.of(basePath, dirName);
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
