package com.evolution.dropfiledaemon.tunnel.framework;

import java.util.UUID;

public record TunnelRequestDTO(String fingerprint,
                               byte[] payload,
                               byte[] nonce) {

    public record Payload(UUID requestId,
                          String command,
                          byte[] payload,
                          Configuration configuration,
                          long timestamp) {
    }

    public record Configuration(boolean compress) {
    }
}
