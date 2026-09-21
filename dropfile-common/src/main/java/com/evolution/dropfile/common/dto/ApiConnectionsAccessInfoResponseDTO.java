package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiConnectionsAccessInfoResponseDTO(String id,
                                                  String key,
                                                  long ttlMillis,
                                                  Instant expiredAt,
                                                  Instant created) {
}
