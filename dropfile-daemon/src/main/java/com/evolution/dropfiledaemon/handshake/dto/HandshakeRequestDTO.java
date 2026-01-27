package com.evolution.dropfiledaemon.handshake.dto;

public record HandshakeRequestDTO(String id,
                                  String payload,
                                  String nonce,
                                  String signature) {

    public record HandshakePayload(String publicKeyRSA,
                                   String publicKeyDH,
                                   long timestamp) {

    }
}
