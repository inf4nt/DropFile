package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeResponseDTO(byte[] payload,
                                   byte[] nonce,
                                   byte[] signature) {

    public record HandshakePayload(byte[] publicKeyRSA,
                                   byte[] publicKeyDH,
                                   String tunnelAlgorithm,
                                   long timestamp) {

    }
}
