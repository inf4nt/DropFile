package com.evolution.dropfile.common.dto;

import java.util.Map;

public record DaemonInfoResponseDTO(
        Map<String, String> systemInfo,
        Map<String, String> configuration) {
}
