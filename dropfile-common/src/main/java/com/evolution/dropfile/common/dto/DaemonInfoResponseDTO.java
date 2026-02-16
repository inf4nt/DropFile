package com.evolution.dropfile.common.dto;

import java.util.Map;

public record DaemonInfoResponseDTO(
        Map<String, Object> systemInfo,
        Object configuration) {
}
