package com.evolution.dropfile.common.dto;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record HandshakeApiTrustInResponseDTO(String remoteFingerprint,
                                             String publicRSA,
                                             String remotePublicRSA,
                                             @Nullable UUID sessionId,
                                             Instant created,
                                             Instant updated) {
}
