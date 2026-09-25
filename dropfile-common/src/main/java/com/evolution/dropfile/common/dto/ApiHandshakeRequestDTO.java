package com.evolution.dropfile.common.dto;

public record ApiHandshakeRequestDTO(String address,
                                     String secretAccessKey,
                                     String alias,
                                     boolean force) {
}
