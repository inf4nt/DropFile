package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiShareInfoResponseDTO(String id,
                                      String alias,
                                      String absoluteFilePath,
                                      String size,
                                      Instant created) {
}
