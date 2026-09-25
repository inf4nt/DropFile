package com.evolution.dropfile.common.dto;

import jakarta.annotation.Nullable;

public record ApiHandshakeRequestDTO(String address,
                                     String secretAccessKey,
                                     @Nullable String alias,
                                     boolean force) {
}
