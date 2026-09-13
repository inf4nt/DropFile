package com.evolution.dropfile.common.crypto;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface CryptoTunnel {

    String getAlgorithm();

    SecretKey secretKey(byte[] rawSecret);

    SecureEnvelope encrypt(byte[] data, SecretKey key);

    byte[] decrypt(byte[] payload, byte[] nonce, SecretKey key);

    OutputStream encryptWrapper(OutputStream outputStream, SecretKey key);

    byte[] decrypt(InputStream inputStream, SecretKey key);

    InputStream decryptStreaming(InputStream inputStream, SecretKey key);

    byte[] readNonce(InputStream inputStream) throws IOException;
}
