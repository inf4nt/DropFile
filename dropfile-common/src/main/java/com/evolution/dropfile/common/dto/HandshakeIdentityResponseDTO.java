package com.evolution.dropfile.common.dto;

public record HandshakeIdentityResponseDTO(HandshakeIdentityPayload payload,
                                           String signature) {

    public record HandshakeIdentityPayload(String publicKey) {

    }
}
