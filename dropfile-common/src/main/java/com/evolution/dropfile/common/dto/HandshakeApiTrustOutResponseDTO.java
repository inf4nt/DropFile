package com.evolution.dropfile.common.dto;

public record HandshakeApiTrustOutResponseDTO(String fingerprint,
                                              String publicKeyDH,
                                              String addressURI) {

}
