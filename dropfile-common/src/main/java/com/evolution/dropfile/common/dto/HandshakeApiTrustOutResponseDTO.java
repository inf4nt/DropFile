package com.evolution.dropfile.common.dto;

import java.time.Instant;

@Deprecated
public record HandshakeApiTrustOutResponseDTO(String fingerprint,
                                              String publicKeyRSA,
                                              String publicKeyDH,
                                              String addressURI,
                                              Instant updated) {

}
