package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeRequestDTO(String id,
                                  byte[] payload,
                                  byte[] nonce,
                                  byte[] signature) {

    public record Payload(byte[] publicKeyRSA,
                          byte[] publicKeyDH,
                          long timestamp) {

    }
}
