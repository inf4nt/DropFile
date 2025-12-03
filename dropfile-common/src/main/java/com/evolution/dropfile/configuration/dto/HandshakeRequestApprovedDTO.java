package com.evolution.dropfile.configuration.dto;

public record HandshakeRequestApprovedDTO(byte[] publicKey,
                                          byte[] encryptMessage) {
}
