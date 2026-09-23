package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.store.framework.file.DirectoryProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class SafePathResolverHelper {

    private final DirectoryProvider daemonConfigDirectoryProvider;

    public String sanitizeFilename(String rawFilename) {
        if (!StringUtils.hasText(rawFilename)) {
            throw new IllegalArgumentException("Unable to sanitize. Argument rawFilename is empty");
        }

        String name = FilenameUtils.getName(rawFilename);

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Filename after sanitization is empty");
        }

        name = name.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("^[.]+", "")
                .replaceAll("[\\p{C}]", "");

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Filename after regex is empty");
        }

        name = name.trim();

        if (!StringUtils.hasText(name) || name.matches("^[.\\s]+$")) {
            throw new IllegalArgumentException("Filename is empty or consists only of dots and spaces");
        }

        if (name.getBytes(StandardCharsets.UTF_8).length > 200) {
            throw new IllegalArgumentException("Request filename must be 200 bytes or less in UTF-8");
        }

        return name;
    }

    public void validateSensitiveDaemonPath(Path path) {
        if (isSensitiveDaemonPath(path)) {
            throw new SecurityException(
                    "Access to daemon configuration or its parent directory is forbidden: " + path
            );
        }
    }

    public boolean isSensitiveDaemonPath(Path path) {
        Objects.requireNonNull(path, "path must not be null");

        List<Path> sensitiveRoots = getCanonicalSensitiveRoots();

        Path targetReal = toRealPath(path);

        for (Path sensitiveRoot : sensitiveRoots) {
            if (targetReal.equals(sensitiveRoot) || targetReal.startsWith(sensitiveRoot)) {
                return true;
            }

            if (sensitiveRoot.startsWith(targetReal)) {
                return true;
            }
        }

        return false;
    }

    private List<Path> getCanonicalSensitiveRoots() {
        Path configDir = daemonConfigDirectoryProvider.getDirectoryPath();

        Path realConfigDir = toRealPath(configDir);

        return List.of(realConfigDir);
    }

    private Path toRealPath(Path path) {
        try {
            if (Files.notExists(path)) {
                throw new FileNotFoundException("Path does not exist: " + path);
            }

            return path.toAbsolutePath().normalize().toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    public Path safeExistingRealPathRegularFileResolver(Path source) {
        try {
            if (!Files.exists(source)) {
                throw new FileNotFoundException("No file found: " + source);
            }

            Path realPath = source.toRealPath();

            if (!Files.isRegularFile(realPath)) {
                throw new IllegalArgumentException("File path is not a regular file: " + source);
            }

            validateSensitiveDaemonPath(realPath);

            return realPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}