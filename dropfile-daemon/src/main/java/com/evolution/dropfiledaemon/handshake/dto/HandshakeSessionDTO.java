package com.evolution.dropfiledaemon.handshake.dto;

public class HandshakeSessionDTO {

    public record Session(String fingerprint,
                          byte[] payload,
                          byte[] signature) {
    }

    public record SessionPayload(byte[] publicKey, long timestamp) {

    }
}
