package com.evolution.dropfile.common.dto;

public record HandshakeRequestDTO(HandshakePayload payload,
                                  String signature) {

    public record HandshakePayload(String publicKeyRSA,
                                   String publicKeyDH,
                                   long timestamp) {

    }
}
