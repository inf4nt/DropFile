package com.evolution.dropfiledaemon.util;

import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SafePathResolver {

    public static String sanitizeFilename(String rawFilename) {
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

        if (name.matches("^[.\\s]+$")) {
            throw new IllegalArgumentException("Filename consists only of dots and spaces");
        }

        if (StringUtils.hasText(name)) {
            return name;
        }

        throw new IllegalArgumentException("Unable to build safe filename");
    }

    public static Path safeRealPathRegularFileResolver(String resourcePath) throws IOException {
        if (!StringUtils.hasText(resourcePath)) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }

        Path path = Paths.get(resourcePath).toAbsolutePath().normalize();

        if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("No file found: " + resourcePath);
        }

        if (Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Symbolic links are not allowed: " + resourcePath);
        }

        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("File path is not a regular file: " + resourcePath);
        }

        return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
    }
}
