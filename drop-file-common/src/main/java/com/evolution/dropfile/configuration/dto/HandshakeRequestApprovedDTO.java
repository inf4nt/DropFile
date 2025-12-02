package com.evolution.dropfile.configuration.dto;

public record HandshakeRequestApprovedDTO(byte[] publicKey,
                                          int port,
                                          byte[] encryptMessage) {
}
