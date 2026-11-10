package com.evolution.dropfiledaemon.handshake.dto;

import java.util.UUID;

public class HandshakeReadinessDTO {

    public record ReadinessRequest(String fingerprint,
                                   byte[] payload,
                                   byte[] signature) {
    }

    public record ReadinessResponse(String fingerprint,
                                    byte[] payload,
                                    byte[] signature) {
    }

    public record ReadinessRequestPayload(UUID requestId,
                                          long timestamp) {
    }

    public record ReadinessResponsePayload(UUID requestId) {
    }
}
