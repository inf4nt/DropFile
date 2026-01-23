package com.evolution.dropfiledaemon.tunnel.framework;

public record TunnelRequestDTO(String fingerprint,
                               byte[] requestPayload,
                               byte[] nonce) {

    public record TunnelRequestPayload(String command,
                                       byte[] payload,
                                       long timestamp) {
    }
}
