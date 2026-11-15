package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeSessionResponseDTO(String fingerprint,
                                          byte[] payload,
                                          byte[] signature) {

    public HandshakeSessionResponseDTO {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId,
                          byte[] serverSalt,
                          byte[] publicKeyDH) {

        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
            Objects.requireNonNull(serverSalt, "serverSalt must not be null");
            Objects.requireNonNull(publicKeyDH, "publicKeyDH must not be null");
        }
    }
}
