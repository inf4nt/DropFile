package com.evolution.dropfile.common.dto;

import java.time.Instant;

@Deprecated
public record ApiShareInfoResponseDTO(String id,
                                      String alias,
                                      String absoluteFilePath,
                                      String size,
                                      Instant created) {
}
