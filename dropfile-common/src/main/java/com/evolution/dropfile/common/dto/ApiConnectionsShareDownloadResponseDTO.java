package com.evolution.dropfile.common.dto;

@Deprecated
public record ApiConnectionsShareDownloadResponseDTO(String operationId,
                                                     String fingerprint,
                                                     String fileId,
                                                     String filename) {
}
