package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiConnectionsAccessInfoResponseDTO(String id,
                                                  String key,
                                                  long ttlMillis,
                                                  Instant expiredAt,
                                                  List<String> commands,
                                                  Instant created) {
}
