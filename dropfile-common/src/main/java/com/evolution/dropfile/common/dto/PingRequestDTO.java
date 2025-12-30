package com.evolution.dropfile.common.dto;

public record PingRequestDTO(String fingerprint,
                             byte[] payload,
                             byte[] nonce) {

    public record PingRequestPayload(long timestamp) {

    }
}
