package com.evolution.dropfile.store.download;

import lombok.With;

import java.time.Instant;

// TODO added Create and Finish date
@With
public record DownloadFileEntry(String fingerprint,
                                String fileId,
                                String destinationFile,
                                String temporaryFile,
                                String hash,
                                long total,
                                long downloaded,
                                DownloadFileEntryStatus status,
                                Instant updated) {

    public DownloadFileEntry(String fingerprintConnection,
                             String fileId,
                             String destinationFile,
                             String temporaryFile,
                             DownloadFileEntryStatus status,
                             Instant updated) {
        this(fingerprintConnection, fileId, destinationFile, temporaryFile, null, 0, 0, status, updated);
    }

    public enum DownloadFileEntryStatus {
        DOWNLOADING,
        ERROR,
        STOPPED,
        COMPLETED
    }
}
