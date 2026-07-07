package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiConnectionsShareLsResponseDTO(String id,
                                               String alias,
                                               String resourcePath,
                                               String size,
                                               Instant created) {
}
