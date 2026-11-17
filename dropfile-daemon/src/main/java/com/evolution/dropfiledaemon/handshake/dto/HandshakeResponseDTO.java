package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeResponseDTO(byte[] payload,
                                   byte[] nonce,
                                   byte[] signature) {

    public HandshakeResponseDTO {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(nonce, "nonce must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId,
                          byte[] serverSalt,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH) {

        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
            Objects.requireNonNull(serverSalt, "serverSalt must not be null");
            Objects.requireNonNull(publicKeyRSA, "publicKeyRSA must not be null");
            Objects.requireNonNull(publicKeyDH, "publicKeyDH must not be null");
        }
    }
}
