package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeReadinessRequestDTO(String fingerprint,
                                           byte[] payload,
                                           byte[] signature) {

    public HandshakeReadinessRequestDTO {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId,
                          long timestamp) {

        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
        }
    }
}
