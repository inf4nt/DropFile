package com.evolution.dropfile.common.dto;

public record HandshakeApiTrustOutResponseDTO(String fingerprint,
                                              String publicKeyRSA,
                                              String publicKeyDH,
                                              String addressURI) {

}
