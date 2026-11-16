package com.evolution.dropfile.common.dto;

import java.util.List;
import java.util.Map;

public record DaemonInfoResponseDTO(
        Map<String, String> systemInfo,
        Map<String, String> configuration,
        Map<String, List<Map<String, String>>> interfaces) {
}
