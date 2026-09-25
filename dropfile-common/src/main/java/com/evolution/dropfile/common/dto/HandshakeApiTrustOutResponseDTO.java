package com.evolution.dropfile.common.dto;

import jakarta.annotation.Nullable;

import java.time.Instant;

public record HandshakeApiTrustOutResponseDTO(String remoteFingerprint,
                                              String publicRSA,
                                              String remotePublicRSA,
                                              String publicDH,
                                              String remotePublicDH,
                                              String addressURI,
                                              @Nullable String alias,
                                              Instant created,
                                              Instant updated) {
}
