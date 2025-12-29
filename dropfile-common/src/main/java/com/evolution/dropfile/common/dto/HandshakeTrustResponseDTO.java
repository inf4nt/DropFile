package com.evolution.dropfile.common.dto;

@Deprecated
public record HandshakeTrustResponseDTO(String publicKey,
                                        String encryptMessage) {
}
