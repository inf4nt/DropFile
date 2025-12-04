package com.evolution.dropfile.configuration.dto;

public record HandshakeStatusInfoDTO(String fingerprint, byte[] publicKey) {
}
