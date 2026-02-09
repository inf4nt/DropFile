package com.evolution.dropfiledaemon.download.exception;

public class ChunkDigestMismatchException extends RuntimeException {
    public ChunkDigestMismatchException(String operation, String expected, String actual, long start, long end) {
        super(String.format(
                "Chunk digest mismatch. Operation: %s expected: %s actual: %s start %s end %s", operation, expected, actual, start, end
        ));
    }
}
