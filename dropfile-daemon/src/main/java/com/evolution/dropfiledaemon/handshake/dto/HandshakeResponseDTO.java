package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeResponseDTO(byte[] payload,
                                   byte[] nonce,
                                   byte[] signature) {

    public record Payload(byte[] publicKeyRSA,
                          byte[] publicKeyDH,
                          long timestamp) {

    }
}
