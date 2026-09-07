package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiQuickShareLsResponseDTO(String id,
                                         String resource,
                                         String size,
                                         String secret,
                                         String relative,
                                         String external,
                                         List<String> wireless,
                                         List<String> ethernet,
                                         boolean directory,
                                         boolean secure,
                                         boolean singleUse,
                                         boolean expired,
                                         Instant updated,
                                         Instant created) {
}
