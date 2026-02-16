package com.evolution.dropfile.common.dto;

public record ApiHandshakeStatusResponseDTO(String fingerprint,
                                            String connection,
                                            @Deprecated String tunnel,
                                            @Deprecated String algorithm) {

}
