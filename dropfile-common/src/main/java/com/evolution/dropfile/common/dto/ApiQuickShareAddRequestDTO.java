package com.evolution.dropfile.common.dto;

public record ApiQuickShareAddRequestDTO(String resourcePath,
                                         String fileAlias,
                                         boolean singleUse,
                                         boolean secure,
                                         String secret) {
}
