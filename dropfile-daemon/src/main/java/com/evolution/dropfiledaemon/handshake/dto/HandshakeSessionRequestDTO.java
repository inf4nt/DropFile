package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeSessionRequestDTO(String fingerprint,
                                         byte[] payload,
                                         byte[] signature) {

    public HandshakeSessionRequestDTO {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId,
                          byte[] clientSalt,
                          byte[] publicKeyDH,
                          long timestamp) {

        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
            Objects.requireNonNull(clientSalt, "clientSalt must not be null");
            Objects.requireNonNull(publicKeyDH, "publicKeyDH must not be null");
        }
    }
}
