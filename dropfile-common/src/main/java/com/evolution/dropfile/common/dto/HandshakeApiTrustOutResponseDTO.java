package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record HandshakeApiTrustOutResponseDTO(String remoteFingerprint,
                                              String publicKeyRSA,
                                              String remotePublicRSA,
                                              String publicKeyDH,
                                              String remotePublicKeyDH,
                                              String addressURI,
                                              Instant created,
                                              Instant updated) {
}
