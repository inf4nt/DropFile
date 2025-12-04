package com.evolution.dropfile.configuration.dto;

public record HandshakeApproveDTO(byte[] publicKey,
                                  byte[] encryptMessage) {
}
