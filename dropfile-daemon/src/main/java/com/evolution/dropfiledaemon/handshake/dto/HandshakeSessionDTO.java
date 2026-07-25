package com.evolution.dropfiledaemon.handshake.dto;

public class HandshakeSessionDTO {

    public record Session(String fingerprint,
                          byte[] payload,
                          byte[] signature) {
    }

    public record SessionRequestPayload(String requestId,
                                        byte[] publicKeyDH,
                                        long timestamp) {
    }

    public record SessionResponsePayload(String requestId,
                                         byte[] publicKeyDH) {
    }
}
