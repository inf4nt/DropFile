package com.evolution.dropfiledaemon.download.exception;

public class ChecksumMismatchException extends RuntimeException {
    public ChecksumMismatchException(String operation, String expected, String actual) {
        super(String.format(
                "Checksum mismatch. Operation: %s expected: %s actual: %s", operation, expected, actual
        ));
    }
}
