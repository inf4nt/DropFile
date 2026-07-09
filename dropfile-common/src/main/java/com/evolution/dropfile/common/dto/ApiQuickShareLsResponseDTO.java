package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiQuickShareLsResponseDTO(String id,
                                         String alias,
                                         String resourcePath,
                                         String size,
                                         String secret,
                                         String relative,
                                         List<String> external,
                                         List<String> wireless,
                                         List<String> ethernet,
                                         boolean secure,
                                         boolean singleUse,
                                         boolean expired,
                                         Instant updated,
                                         Instant created) {
}
