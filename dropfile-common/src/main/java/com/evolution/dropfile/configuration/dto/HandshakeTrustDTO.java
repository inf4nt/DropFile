package com.evolution.dropfile.configuration.dto;

public record HandshakeTrustDTO(byte[] publicKey,
                                byte[] encryptMessage) {
}
