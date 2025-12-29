package com.evolution.dropfile.common.dto;

public record HandshakeIdentityResponseDTO(String publicKey,
                                           String fingerprintSignature) {
}
