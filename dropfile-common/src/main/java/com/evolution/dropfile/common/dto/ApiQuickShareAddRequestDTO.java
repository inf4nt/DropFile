package com.evolution.dropfile.common.dto;

public record ApiQuickShareAddRequestDTO(String resource,
                                         boolean singleUse,
                                         boolean secure,
                                         String secret) {
}
