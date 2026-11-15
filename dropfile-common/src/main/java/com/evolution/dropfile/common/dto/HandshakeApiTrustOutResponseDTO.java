package com.evolution.dropfile.common.dto;

import jakarta.annotation.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HandshakeApiTrustOutResponseDTO(String remoteFingerprint,
                                              String publicRSA,
                                              String remotePublicRSA,
                                              @Nullable UUID handshakeId,
                                              List<AddressEnvelope> addressEnvelopes,
                                              Instant created,
                                              Instant updated) {

    public record AddressEnvelope(URI address, Instant lastSeen) {

    }
}
