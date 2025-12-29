package com.evolution.dropfile.common.dto;

public record HandshakeResponseDTO(HandshakePayload payload,
                                   String signature) {

    public record HandshakePayload(String publicKeyRSA,
                                   String publicKeyDH,
                                   HandshakeStatus status,
                                   long timestamp) {

    }

    public enum HandshakeStatus {
        PENDING,
        APPROVED
    }
}
