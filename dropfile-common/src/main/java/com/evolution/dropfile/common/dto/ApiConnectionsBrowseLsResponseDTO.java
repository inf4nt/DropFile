package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiConnectionsBrowseLsResponseDTO(String id,
                                                String alias,
                                                String size,
                                                Instant created) {
}
