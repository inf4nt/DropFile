package com.evolution.dropfiledaemon.tunnel.framework;

public record TunnelRequestDTO(String fingerprint,
                               byte[] payload,
                               byte[] nonce) {

    public record Payload(String command,
                          byte[] payload,
                          Configuration configuration,
                          long timestamp) {
    }

    public record Configuration(boolean compress) {
    }
}
