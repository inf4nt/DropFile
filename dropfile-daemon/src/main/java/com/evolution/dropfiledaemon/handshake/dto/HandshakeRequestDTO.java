package com.evolution.dropfiledaemon.handshake.dto;

import java.util.UUID;

public record HandshakeRequestDTO(String accessKeyId,
                                  byte[] payload,
                                  byte[] nonce,
                                  byte[] signature) {

    public record Payload(UUID requestId,
                          byte[] clientSalt,
                          byte[] publicKeyRSA,
                          byte[] publicKeyDH,
                          long timestamp) {

    }
}
