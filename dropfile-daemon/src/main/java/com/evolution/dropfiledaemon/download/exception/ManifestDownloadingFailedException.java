package com.evolution.dropfiledaemon.download.exception;

public class ManifestDownloadingFailedException extends RuntimeException {
    public ManifestDownloadingFailedException(String operation, String fileId, String filename, Exception cause) {
        super(String.format(
                "Manifest downloading failed. Operation: %s file id: %s filename: %s",
                operation, fileId, filename
        ), cause);
    }
}
