package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeResponseDTO(String payload,
                                   String nonce,
                                   String signature) {

    public record HandshakePayload(String publicKeyRSA,
                                   String publicKeyDH,
                                   HandshakeStatus status,
                                   String tunnelAlgorithm,
                                   long timestamp) {

    }

    public enum HandshakeStatus {
        OK
    }
}
