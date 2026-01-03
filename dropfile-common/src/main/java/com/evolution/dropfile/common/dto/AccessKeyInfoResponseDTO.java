package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record AccessKeyInfoResponseDTO(String id,
                                       String key,
                                       Instant created) {
}
