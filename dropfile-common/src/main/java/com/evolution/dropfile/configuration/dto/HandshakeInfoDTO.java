package com.evolution.dropfile.configuration.dto;

public record HandshakeInfoDTO(String fingerprint, byte[] publicKey) {
}
