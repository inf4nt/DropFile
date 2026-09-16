package com.evolution.dropfiledaemon.handshake.dto;

import java.util.UUID;

public record HandshakeRequestDTO(String accessKeyId,
                                  byte[] payload,
                                  byte[] nonce,
                                  byte[] signature) {

    // TODO byte[] clientNonce
    public record Payload(UUID requestId,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH,
                          long timestamp) {

    }
}
