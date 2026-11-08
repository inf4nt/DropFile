package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.Collection;

public record ApiConnectionsAccessInfoResponseDTO(String id,
                                                  String key,
                                                  long ttlMillis,
                                                  Instant expiredAt,
                                                  Collection<String> commands,
                                                  Instant created) {
}
