package com.evolution.dropfile.common.dto;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record HandshakeApiTrustOutResponseDTO(String remoteFingerprint,
                                              String publicRSA,
                                              String remotePublicRSA,
                                              String publicDH,
                                              String remotePublicDH,
                                              List<AddressEnvelope> addressEnvelopes,
                                              Instant created,
                                              Instant updated) {

    public record AddressEnvelope(URI address, Instant lastSeen) {

    }
}
