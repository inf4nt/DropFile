package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiDownloadLsResponse(String operation,
                                    String fileId,
                                    String file,
                                    String hash,
                                    long downloaded,
                                    long total,
                                    String percentage,
                                    Status status,
                                    Instant updated) {

    public enum Status {
        DOWNLOADING,
        COMPLETED,
        STOPPED,
        ERROR
    }
}
