package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record HandshakeApiTrustInResponseDTO(String fingerprint,
                                             String publicKeyRSA,
                                             String publicKeyDH,
                                             Instant updated) {

}
