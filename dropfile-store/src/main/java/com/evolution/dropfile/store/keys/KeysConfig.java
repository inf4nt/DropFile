package com.evolution.dropfile.store.keys;

public record KeysConfig(Keys rsa, Keys dh) {

    public record Keys(byte[] publicKey, byte[] privateKey) {

    }
}
