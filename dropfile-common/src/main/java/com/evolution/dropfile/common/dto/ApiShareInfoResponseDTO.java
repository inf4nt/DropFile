package com.evolution.dropfile.common.dto;

// TODO add file size
public record ApiShareInfoResponseDTO(String id,
                                      String alias,
                                      String absoluteFilePath) {
}
