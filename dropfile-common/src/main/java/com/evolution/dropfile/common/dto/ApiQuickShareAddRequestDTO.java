package com.evolution.dropfile.common.dto;

public record ApiQuickShareAddRequestDTO(String resourcePath,
                                         String alias,
                                         boolean singleUse,
                                         boolean secure,
                                         String secret) {
}
