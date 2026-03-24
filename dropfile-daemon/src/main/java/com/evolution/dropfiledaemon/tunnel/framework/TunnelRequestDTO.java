package com.evolution.dropfiledaemon.tunnel.framework;

public record TunnelRequestDTO(String fingerprint,
                               byte[] payload,
                               byte[] nonce) {

    public record TunnelRequestPayload(String command,
                                       byte[] payload,
                                       TunnelRequestConfiguration configuration,
                                       long timestamp) {
    }

    public record TunnelRequestConfiguration(boolean compress) {
    }
}
