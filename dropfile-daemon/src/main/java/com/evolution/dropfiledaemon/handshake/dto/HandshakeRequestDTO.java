package com.evolution.dropfiledaemon.handshake.dto;

import java.util.Objects;
import java.util.UUID;

public record HandshakeRequestDTO(String accessKeyId,
                                  byte[] payload,
                                  byte[] nonce,
                                  byte[] signature) {

    public HandshakeRequestDTO(String accessKeyId, byte[] payload, byte[] nonce, byte[] signature) {
        this.accessKeyId = Objects.requireNonNull(accessKeyId, "accessKeyId must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.nonce = Objects.requireNonNull(nonce, "nonce must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
    }

    public record Payload(UUID requestId,
                          byte[] clientSalt,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH,
                          long timestamp) {
        public Payload(UUID requestId, byte[] clientSalt, byte[] publicKeyRSA, byte[] publicKeyDH, long timestamp) {
            this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
            this.clientSalt = Objects.requireNonNull(clientSalt, "clientSalt must not be null");
            this.publicKeyRSA = Objects.requireNonNull(publicKeyRSA, "publicKeyRSA must not be null");
            this.publicKeyDH = Objects.requireNonNull(publicKeyDH, "publicKeyDH must not be null");
            this.timestamp = timestamp;
        }
    }
}
