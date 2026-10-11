package com.evolution.dropfiledaemon.download.procedure;


import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FileCompressionPolicyHelper {

    private static final Set<String> EXCLUDED_EXTENSIONS = Set.of(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "tgz", "tbz2",
            "jpg", "jpeg", "png", "gif", "webp", "mp4", "mkv", "avi", "mov", "mp3", "aac", "flac", "m4a",
            "iso", "img", "bin", "dmg", "pkg", "deb", "rpm",
            "pdf"
    ).stream().map(it -> "." + it).collect(Collectors.toSet());

    public static boolean isCompressible(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename must not be empty string");
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);

        for (String ext : EXCLUDED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return false;
            }
        }

        return true;
    }
}
