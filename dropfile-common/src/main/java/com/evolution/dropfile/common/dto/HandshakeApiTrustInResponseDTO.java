package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record HandshakeApiTrustInResponseDTO(String remoteFingerprint,
                                             String publicRSA,
                                             String remotePublicRSA,
                                             String publicDH,
                                             String remotePublicDH,
                                             Instant created,
                                             Instant updated) {
}
