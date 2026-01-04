package com.evolution.dropfile.common.dto;

public record DownloadFileResponseDTO(String id,
                                      String alias,
                                      byte[] payload) {
}
