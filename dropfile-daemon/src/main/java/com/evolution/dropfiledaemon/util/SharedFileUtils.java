package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.store.share.ShareFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class SharedFileUtils {

    public static boolean isAccessible(ShareFile shareFile) {
        if (!shareFile.accessible()) {
            return false;
        }

        Path path = Paths.get(shareFile.resourcePath());

        if (Files.notExists(path)) {
            return false;
        }

        try {
            long currentSize = Files.size(path);
            Instant currentLastModified = Files.getLastModifiedTime(path).toInstant();
            return currentSize == shareFile.size()
                    && currentLastModified.equals(shareFile.fileLastModified());
        } catch (IOException e) {
            return false;
        }
    }
}
