package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record HandshakeApiTrustOutResponseDTO(String fingerprint,
                                              String publicKeyRSA,
                                              String publicKeyDH,
                                              String addressURI,
                                              Instant updated) {

}
