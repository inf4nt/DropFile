package com.evolution.dropfiledaemon.handshake.dto;

import java.util.UUID;

public record HandshakeResponseDTO(byte[] payload,
                                   byte[] nonce,
                                   byte[] signature) {

    public record Payload(UUID requestId,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH) {

    }
}
