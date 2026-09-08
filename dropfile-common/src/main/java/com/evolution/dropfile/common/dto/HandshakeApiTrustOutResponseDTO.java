package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record HandshakeApiTrustOutResponseDTO(String remoteFingerprint,
                                              String publicRSA,
                                              String remotePublicRSA,
                                              String publicDH,
                                              String remotePublicDH,
                                              String addressURI,
                                              Instant created,
                                              Instant updated) {
}
