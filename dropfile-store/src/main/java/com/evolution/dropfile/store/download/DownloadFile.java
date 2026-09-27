package com.evolution.dropfile.store.download;

import lombok.With;

import java.time.Instant;

@With
public record DownloadFile(String fingerprint,
                           String fileId,
                           String destinationFile,
                           String temporaryFile,
                           String hash,
                           long total,
                           long downloaded,
                           DownloadFileStatus status,
                           Instant created,
                           Instant updated) {

    public DownloadFile(String fingerprint,
                        String fileId,
                        String destinationFile,
                        String temporaryFile,
                        DownloadFileStatus status,
                        Instant created,
                        Instant updated) {
        this(fingerprint, fileId, destinationFile, temporaryFile, null, 0, 0, status, created, updated);
    }

    public enum DownloadFileStatus {
        DOWNLOADING,
        ERROR,
        STOPPED,
        COMPLETED,
        INTERRUPTED
    }
}
