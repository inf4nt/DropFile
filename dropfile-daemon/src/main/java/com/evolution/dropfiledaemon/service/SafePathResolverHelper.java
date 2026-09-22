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
import java.nio.file.Paths;
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
        Objects.requireNonNull(path, "path must not be null");

        Path configDir = daemonConfigDirectoryProvider.getDirectoryPath()
                .toAbsolutePath()
                .normalize();

        Path target;
        try {
            target = path.toRealPath();
        } catch (IOException e) {
            target = path.toAbsolutePath().normalize();
        }

        if (target.equals(configDir) || target.startsWith(configDir)) {
            throw new SecurityException(
                    "Access to daemon configuration directory is forbidden: " + target
            );
        }

        if (configDir.startsWith(target)) {
            throw new SecurityException(
                    "Sharing parent directory of daemon configuration is forbidden: " + target
            );
        }
    }

    public boolean isSensitiveDaemonPath(Path path) {
        try {
            Path configDir = daemonConfigDirectoryProvider.getDirectoryPath().toRealPath();
            Path target = path.toRealPath();
            return target.equals(configDir) || target.startsWith(configDir);
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