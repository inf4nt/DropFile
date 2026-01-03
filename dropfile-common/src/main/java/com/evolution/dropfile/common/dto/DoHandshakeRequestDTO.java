package com.evolution.dropfile.common.dto;

public record DoHandshakeRequestDTO(String id,
                                    String payload,
                                    String nonce) {

    public record DoHandshakePayload(String publicKeyDH,
                                     long timestamp) {

    }
}
