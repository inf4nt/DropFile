package com.evolution.dropfile.common.dto;

import java.time.Instant;

public class ApiDownloadLsDTO {

    public record Response(String operation,
                           String fileId,
                           String file,
                           String hash,
                           String total,
                           String downloaded,
                           String percent,
                           ApiDownloadLsDTO.Status status,
                           Instant updated) {

    }

    public record Request(Status status, Integer limit) {

    }

    public enum Status {
        DOWNLOADING,
        COMPLETED,
        STOPPED,
        ERROR
    }
}
