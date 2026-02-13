package com.evolution.dropfile.store.download;

import java.time.Instant;

public record DownloadFileEntry(String fingerprintConnection,
                                String fileId,
                                String destinationFile,
                                String temporaryFile,
                                String hash,
                                long downloaded,
                                long total,
                                DownloadFileEntryStatus status,
                                Instant updated) {

    public enum DownloadFileEntryStatus {
        DOWNLOADING,
        ERROR,
        STOPPED,
        COMPLETED
    }
}
