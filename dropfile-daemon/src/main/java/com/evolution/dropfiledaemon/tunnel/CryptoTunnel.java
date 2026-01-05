package com.evolution.dropfiledaemon.tunnel;

import javax.crypto.SecretKey;

public interface CryptoTunnel {

    String getAlgorithm();

    SecretKey secretKey(byte[] secret);

    SecureEnvelope encrypt(byte[] data, SecretKey key);

    byte[] decrypt(byte[] payload, byte[] nonce, SecretKey key);
}
