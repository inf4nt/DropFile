package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeReadinessResponseDTO(String fingerprint,
                                            byte[] payload,
                                            byte[] signature) {

    public HandshakeReadinessResponseDTO {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId) {

        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
        }
    }
}
