package com.evolution.dropfile.common.dto;

public record ApiQuickShareAddRequestDTO(String resourcePath,
                                         boolean singleUse,
                                         boolean secure,
                                         String secret) {
}
