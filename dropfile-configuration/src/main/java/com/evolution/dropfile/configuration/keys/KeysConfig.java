package com.evolution.dropfile.configuration.keys;

public record KeysConfig(Keys dh) {

    public record Keys(byte[] publicKey, byte[] privateKey) {

    }
}
