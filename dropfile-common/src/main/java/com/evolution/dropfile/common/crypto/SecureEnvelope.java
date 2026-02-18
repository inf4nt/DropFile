package com.evolution.dropfile.common.crypto;

public record SecureEnvelope(byte[] payload, byte[] nonce) {
}
