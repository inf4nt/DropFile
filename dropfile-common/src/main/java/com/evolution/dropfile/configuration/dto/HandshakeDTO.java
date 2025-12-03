package com.evolution.dropfile.configuration.dto;

public record HandshakeDTO(byte[] publicKey,
                           byte[] encryptMessage) {
}
