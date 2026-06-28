package com.evolution.dropfile.common.dto;

public record ApiConnectionsBrowseGetResponseDTO(String operationId,
                                                 String fingerprint,
                                                 String fileId,
                                                 String filename) {
}
