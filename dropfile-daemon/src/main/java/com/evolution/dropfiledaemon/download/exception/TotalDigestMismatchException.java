package com.evolution.dropfiledaemon.download.exception;

public class TotalDigestMismatchException extends RuntimeException {
    public TotalDigestMismatchException(String operation, String expected, String actual) {
        super(String.format(
                "Total digest mismatch. Operation: %s expected: %s actual: %s", operation, expected, actual
        ));
    }
}
