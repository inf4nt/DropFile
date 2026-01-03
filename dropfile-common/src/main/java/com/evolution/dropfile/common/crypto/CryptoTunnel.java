package com.evolution.dropfile.common.crypto;

import javax.crypto.SecretKey;

public interface CryptoTunnel {

    String getAlgorithm();

    SecretKey secretKey(byte[] secret);

    SecureEnvelope encrypt(byte[] data, SecretKey key);

    default byte[] decrypt(SecureEnvelope envelope, SecretKey key) {
        return decrypt(envelope.payload(), envelope.nonce(), key);
    }

    byte[] decrypt(byte[] payload, byte[] nonce, SecretKey key);
}
