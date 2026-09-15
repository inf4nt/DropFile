package com.evolution.dropfiledaemon.handshake.dto;

import java.util.UUID;

public class HandshakeSessionDTO {

    public record Session(String fingerprint,
                          byte[] payload,
                          byte[] signature) {
    }

    public record SessionRequestPayload(UUID requestId,
                                        byte[] publicKeyDH,
                                        long timestamp) {
    }

    public record SessionResponsePayload(UUID requestId,
                                         byte[] publicKeyDH) {
    }
}
