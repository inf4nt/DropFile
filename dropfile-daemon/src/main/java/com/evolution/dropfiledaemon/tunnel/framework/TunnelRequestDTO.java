package com.evolution.dropfiledaemon.tunnel.framework;

import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

public record TunnelRequestDTO(String fingerprint,
                               byte[] payload,
                               byte[] nonce) {

    public TunnelRequestDTO {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(nonce, "nonce must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(UUID requestId,
                          String command,
                          @Nullable byte[] payload,
                          Configuration configuration,
                          long timestamp) {

        public Payload {
            Objects.requireNonNull(requestId, "requestId must not be null");
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(configuration, "configuration must not be null");
        }
    }

    public record Configuration(boolean compress) {
    }
}
