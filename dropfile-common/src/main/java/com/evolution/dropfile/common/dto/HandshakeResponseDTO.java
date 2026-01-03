package com.evolution.dropfile.common.dto;

public record HandshakeResponseDTO(String payload,
                                   String nonce) {

    public record HandshakePayload(String publicKeyDH,
                                   HandshakeStatus status,
                                   String tunnelAlgorithm,
                                   long timestamp) {

    }

    public enum HandshakeStatus {
        OK
    }
}
