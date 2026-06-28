package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiConnectionsShareAddResponseDTO(String id,
                                                String alias,
                                                String absoluteFilePath,
                                                String size,
                                                Instant created) {
}
