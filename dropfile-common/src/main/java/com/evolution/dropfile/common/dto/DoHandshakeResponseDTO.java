package com.evolution.dropfile.common.dto;

public record DoHandshakeResponseDTO(String payload,
                                     String nonce) {

    public record DoHandshakePayload(String publicKeyDH,
                                     HandshakeStatus status,
                                     long timestamp) {

    }

    public enum HandshakeStatus {
        APPROVED
    }
}
