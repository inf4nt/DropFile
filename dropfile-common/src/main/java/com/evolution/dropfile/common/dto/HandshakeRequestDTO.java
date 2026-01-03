package com.evolution.dropfile.common.dto;

public record HandshakeRequestDTO(String id,
                                  String payload,
                                  String nonce) {

    public record HandshakePayload(String publicKeyDH,
                                   long timestamp) {

    }
}
