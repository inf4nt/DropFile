package com.evolution.dropfiledaemon.download.exception;

public class DownloadingStoppedException extends RuntimeException {
    public DownloadingStoppedException(String operation) {
        super(String.format("Downloading has been stopped. Operation: %s", operation));
    }
}
