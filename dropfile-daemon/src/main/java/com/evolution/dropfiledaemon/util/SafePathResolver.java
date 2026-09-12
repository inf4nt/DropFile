package com.evolution.dropfiledaemon.util;

import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

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
}
