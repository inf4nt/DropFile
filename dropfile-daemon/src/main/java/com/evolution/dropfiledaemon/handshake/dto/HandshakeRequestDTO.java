package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeRequestDTO(String accessKeyId,
                                  byte[] payload,
                                  byte[] nonce,
                                  byte[] signature) {

    public HandshakeRequestDTO {
        Objects.requireNonNull(accessKeyId, "accessKeyId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(nonce, "nonce must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId,
                          byte[] clientSalt,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH,
                          long timestamp) {
        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
            Objects.requireNonNull(clientSalt, "clientSalt must not be null");
            Objects.requireNonNull(publicKeyRSA, "publicKeyRSA must not be null");
            Objects.requireNonNull(publicKeyDH, "publicKeyDH must not be null");
        }
    }
}
