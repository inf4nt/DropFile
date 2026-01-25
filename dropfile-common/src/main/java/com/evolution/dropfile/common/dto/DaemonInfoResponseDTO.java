package com.evolution.dropfile.common.dto;

public record DaemonInfoResponseDTO(
        String fingerprint,
        String publicKeyRSA,
        String publicKeyDH,
        String systemInfo) {
}
