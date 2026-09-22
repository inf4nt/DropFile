package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.store.share.ShareFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Component
public class ApiShareFileHelper {

    public boolean isAccessible(ShareFile shareFile) {
        if (!shareFile.accessible()) {
            return false;
        }

        Path path = Paths.get(shareFile.resourceRealPath());

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
