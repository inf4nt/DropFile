package com.evolution.dropfile.store.keys;

public record KeysConfig(Keys dh) {

    public record Keys(byte[] publicKey, byte[] privateKey) {

    }
}
