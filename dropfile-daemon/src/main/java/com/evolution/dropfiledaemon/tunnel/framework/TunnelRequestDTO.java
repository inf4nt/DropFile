package com.evolution.dropfiledaemon.tunnel.framework;

public record TunnelRequestDTO(String fingerprint,
                               String requestPayload,
                               String nonce) {

    public record TunnelRequestPayload(String action,
                                       byte[] payload,
                                       long timestamp) {
    }
}
