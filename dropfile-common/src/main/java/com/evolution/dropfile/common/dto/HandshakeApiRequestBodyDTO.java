package com.evolution.dropfile.common.dto;

public record HandshakeApiRequestBodyDTO(String nodeAddress,
                                         String publicKeyRSA,
                                         String publicKeyDH) {
}
