package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeResponseDTO(byte[] payload,
                                   byte[] nonce,
                                   byte[] signature) {

    public record HandshakePayload(byte[] publicKeyRSA,
                                   byte[] publicKeyDH,
                                   HandshakeStatus status,
                                   String tunnelAlgorithm,
                                   long timestamp) {

    }

    public enum HandshakeStatus {
        OK
    }
}
