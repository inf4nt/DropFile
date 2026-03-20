package com.evolution.dropfile.common.dto;

import java.util.List;

public record ApiConnectionsShareDownloadResponseDTO(List<Ok> executing, List<Skipped> skipped) {

    public record Ok(String operationId, String fingerprint, String fileId, String filename) {
    }

    public record Skipped(String fingerprint, String fileId, String filename) {
    }
}
