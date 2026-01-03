package com.evolution.dropfile.common.dto;

public record DaemonInfoResponseDTO(
        String fingerprint,
        String publicKeyDH) {
}
