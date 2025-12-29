package com.evolution.dropfile.common.dto;

public record HandshakeApiOutgoingResponseDTO(String fingerprint, String publicKey, String addressURI) {
}
