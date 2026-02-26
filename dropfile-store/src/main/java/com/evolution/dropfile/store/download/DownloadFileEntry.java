package com.evolution.dropfile.store.download;

import lombok.With;

import java.time.Instant;

@With
public record DownloadFileEntry(String fingerprint,
                                String fileId,
                                String destinationFile,
                                String temporaryFile,
                                String hash,
                                long total,
                                long downloaded,
                                DownloadFileEntryStatus status,
                                Instant created,
                                Instant updated) {

    public DownloadFileEntry(String fingerprint,
                             String fileId,
                             String destinationFile,
                             String temporaryFile,
                             DownloadFileEntryStatus status,
                             Instant created,
                             Instant updated) {
        this(fingerprint, fileId, destinationFile, temporaryFile, null, 0, 0, status, created, updated);
    }

    public enum DownloadFileEntryStatus {
        DOWNLOADING,
        ERROR,
        STOPPED,
        COMPLETED,
        INTERRUPTED
    }
}
