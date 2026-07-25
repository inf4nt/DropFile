package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeResponseDTO(byte[] payload,
                                   byte[] nonce,
                                   byte[] signature) {

    public record Payload(String requestId,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH) {

    }
}
