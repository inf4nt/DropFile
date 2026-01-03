package com.evolution.dropfile.common.dto;

@Deprecated
public record HandshakeApiIncomingResponseDTO(String fingerprint, String publicKey, String addressURI) {

}
