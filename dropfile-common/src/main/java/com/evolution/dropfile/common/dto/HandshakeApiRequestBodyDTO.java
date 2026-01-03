package com.evolution.dropfile.common.dto;

@Deprecated
public record HandshakeApiRequestBodyDTO(String nodeAddress,
                                         String publicKeyRSA,
                                         String publicKeyDH) {
}
