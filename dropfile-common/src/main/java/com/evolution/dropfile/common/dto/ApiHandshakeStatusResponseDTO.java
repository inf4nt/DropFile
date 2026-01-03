package com.evolution.dropfile.common.dto;

public record ApiHandshakeStatusResponseDTO(String fingerprint,
                                            String connection,
                                            String tunnel,
                                            String algorithm) {
}
