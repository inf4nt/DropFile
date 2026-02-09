package com.evolution.dropfiledaemon.download.exception;

public class ChunkWritingFailedException extends RuntimeException {
    public ChunkWritingFailedException(String operation, String hash, long start, long end, Exception cause) {
        super(String.format(
                "Chunk writing failed. Operation: %s hash: %s start: %s end: %s",
                operation, hash, start, end
        ), cause);
    }
}
