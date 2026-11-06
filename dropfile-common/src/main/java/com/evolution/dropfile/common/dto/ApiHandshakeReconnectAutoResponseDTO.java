package com.evolution.dropfile.common.dto;

import java.net.URI;

public record ApiHandshakeReconnectAutoResponseDTO(String fingerprint,
                                                   URI address,
                                                   String alias) {
}
