package com.evolution.dropfiledaemon.download.exception;

public class ChunkDownloadingFailedException extends RuntimeException {
    public ChunkDownloadingFailedException(String operation, String hash, long start, long end, Exception cause) {
        super(String.format(
                "Chunk downloading failed. Operation: %s hash: %s start: %s end: %s",
                operation, hash, start, end
        ), cause);
    }
}
