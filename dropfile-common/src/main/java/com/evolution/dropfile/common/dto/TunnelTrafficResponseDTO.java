package com.evolution.dropfile.common.dto;

import java.util.Map;

public record TunnelTrafficResponseDTO(Map<String, String> download, Map<String, String> upload) {
}
