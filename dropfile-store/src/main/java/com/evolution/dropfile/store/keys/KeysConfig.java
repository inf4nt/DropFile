package com.evolution.dropfile.store.keys;

@Deprecated
public record KeysConfig(@Deprecated Keys rsa, @Deprecated Keys dh) {

    @Deprecated
    public record Keys(@Deprecated byte[] publicKey, @Deprecated byte[] privateKey) {

    }
}
