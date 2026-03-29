package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.Map;

public record ApiConnectionsAccessInfoResponseDTO(String id,
                                                  String key,
                                                  Instant created,
                                                  Map<String, String> connectionKeys) {
}
