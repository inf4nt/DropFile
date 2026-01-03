package com.evolution.dropfile.common.dto;

@Deprecated
public record HandshakeApiOutgoingResponseDTO(String fingerprint, String publicKey, String addressURI) {
}
