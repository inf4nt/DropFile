package com.evolution.dropfiledaemon.tunnel;

public record SecureEnvelope(byte[] payload, byte[] nonce) {
}
