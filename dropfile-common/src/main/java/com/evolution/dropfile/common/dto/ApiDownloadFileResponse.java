package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiDownloadFileResponse(String operationId,
                                      String fileId,
                                      String fileAbsolutePath,
                                      String hash,
                                      long downloaded,
                                      long total,
                                      String percentage,
                                      Status status,
                                      Instant updated) {

    public enum Status {
        DOWNLOADING,
        COMPLETED,
        ERROR
    }
}
