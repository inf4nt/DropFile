package com.evolution.dropfiledaemon.tunnel;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;

public interface CryptoTunnel {

    String getAlgorithm();

    SecretKey secretKey(byte[] secret);

    SecureEnvelope encrypt(byte[] data, SecretKey key);

    byte[] decrypt(byte[] payload, byte[] nonce, SecretKey key);

    void encrypt(InputStream inputStream, OutputStream outputStream, SecretKey key);

    InputStream decrypt(InputStream inputStream, SecretKey key);
}
