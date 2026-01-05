package com.evolution.dropfile.common.dto;

public record ApiConnectionsDownloadFileRequestDTO(String id,
                                                   String filename,
                                                   boolean rewrite) {
}
