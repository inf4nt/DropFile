package com.evolution.dropfile.common.dto;

import java.time.Instant;

public class ApiDownloadLsDTO {

    public record Response(String operation,
                           String fingerprint,
                           String fileId,
                           String file,
                           String progress,
                           String speed,
                           ApiDownloadLsDTO.Status status,
                           Instant created,
                           Instant updated) {

    }

    public record Request(Status status, Integer limit) {

    }

    public enum Status {
        DOWNLOADING,
        COMPLETED,
        STOPPED,
        ERROR,
        INTERRUPTED,
        QUEUE
    }
}
