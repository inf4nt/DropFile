package com.evolution.dropfiledaemon.download.exception;

public class DigestMismatchException extends RuntimeException {
    public DigestMismatchException(String operation, String expected, String actual) {
        super(String.format(
                "Digest mismatch. Operation: %s expected: %s actual: %s", operation, expected, actual
        ));
    }
}
