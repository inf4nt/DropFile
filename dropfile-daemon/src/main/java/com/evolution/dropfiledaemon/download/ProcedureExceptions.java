package com.evolution.dropfiledaemon.download;

public class ProcedureExceptions {

    public static void chunkDigestMismatchException(String operation,
                                                    String expectedDigest,
                                                    String actualDigest,
                                                    int size,
                                                    long position) {
        throw new RuntimeException(String.format(
                "Chunk digest mismatch. Operation: %s expected: %s actual: %s size %s position %s",
                operation, expectedDigest, actualDigest, size, position
        ));
    }

    public static void totalDigestMismatchException(String operation, String expectedDigest, String actualDigest) {
        throw new RuntimeException(String.format(
                "Total digest mismatch. Operation: %s expected: %s actual: %s", operation, expectedDigest, actualDigest
        ));
    }
}
