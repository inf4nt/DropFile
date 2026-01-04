package com.evolution.dropfile.common.dto;

@Deprecated
public record HandshakeIdentityResponseDTO(HandshakeIdentityPayload payload,
                                           String signature) {

    public record HandshakeIdentityPayload(String publicKeyRSA, String publicKeyDH) {

    }
}
