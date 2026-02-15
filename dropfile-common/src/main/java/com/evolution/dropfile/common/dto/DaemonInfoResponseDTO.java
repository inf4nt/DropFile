package com.evolution.dropfile.common.dto;

import java.util.Map;

@Deprecated
public record DaemonInfoResponseDTO(
        String fingerprint,
        String publicKeyRSA,
        String publicKeyDH,
        Map<String, Object> systemInfo,
        Object configuration) {
}
