package com.evolution.dropfile.common.dto;

import java.time.Instant;

public record HandshakeApiTrustInResponseDTO(String remoteFingerprint,
                                             String publicKeyRSA,
                                             String remotePublicRSA,
                                             String publicKeyDH,
                                             String remotePublicKeyDH,
                                             Instant created,
                                             Instant updated) {
}
