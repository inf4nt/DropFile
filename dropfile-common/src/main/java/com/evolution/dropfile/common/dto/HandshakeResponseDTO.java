package com.evolution.dropfile.common.dto;

public record HandshakeResponseDTO(HandshakePayload payload,
                                   String signature) {

    public record HandshakePayload(String publicKeyRSA,
                                   String publicKeyDH,
                                   long timestamp) {

    }
}
