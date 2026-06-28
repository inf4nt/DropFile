package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiQuickShareLsResponseDTO(String id,
                                         String alias,
                                         String absolutePath,
                                         String secret,
                                         String relative,
                                         List<String> absolute,
                                         boolean singleUse,
                                         boolean expired,
                                         Instant updated,
                                         Instant created) {
}
