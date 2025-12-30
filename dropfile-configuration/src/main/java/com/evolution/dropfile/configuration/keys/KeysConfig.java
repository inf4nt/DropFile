package com.evolution.dropfile.configuration.keys;

public record KeysConfig(Keys rsa,
                         Keys dh) {

    public record Keys(byte[] publicKey, byte[] privateKey) {

    }
}
