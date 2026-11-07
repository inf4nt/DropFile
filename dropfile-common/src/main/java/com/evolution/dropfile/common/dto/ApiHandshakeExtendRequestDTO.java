package com.evolution.dropfile.common.dto;

import com.evolution.dropfile.common.CriteriaEnvelope;

public record ApiHandshakeExtendRequestDTO(String address,
                                           CriteriaEnvelope fingerprint) {
}
