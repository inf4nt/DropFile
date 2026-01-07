package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record ApiConnectionsAccessInfoResponseDTO(String id,
                                                  String key,
                                                  Instant created) {
}
