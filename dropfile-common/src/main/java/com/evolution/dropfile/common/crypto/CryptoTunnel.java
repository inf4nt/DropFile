package com.evolution.dropfile.common.crypto;

import jakarta.annotation.Nullable;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public interface CryptoTunnel {

    String getAlgorithm();

    SecretKey secretKey(byte[] keyBytes);

    byte[] deriveKeyHkdf(byte[] rawSecret, String info, @Nullable byte[] salt) throws GeneralSecurityException;

    default SecretKey deriveSecretKey(byte[] rawSecret, String info, @Nullable byte[] salt) throws GeneralSecurityException {
        byte[] bytes = deriveKeyHkdf(rawSecret, info, salt);
        return secretKey(bytes);
    }

    default SecretKey deriveSecretKey(byte[] rawSecret, String info) throws GeneralSecurityException {
        return deriveSecretKey(rawSecret, info, null);
    }

    SecureEnvelope encrypt(byte[] payload, byte[] aad, SecretKey secretKey) throws GeneralSecurityException;

    byte[] decrypt(byte[] payload, byte[] nonce, byte[] aad, SecretKey secretKey) throws GeneralSecurityException;

    byte[] decrypt(InputStream inputStream, byte[] aad, SecretKey secretKey) throws GeneralSecurityException, IOException;

    OutputStream encryptWrapper(OutputStream outputStream, byte[] aad, SecretKey secretKey) throws GeneralSecurityException, IOException;

    InputStream decryptStreaming(InputStream inputStream, byte[] aad, SecretKey secretKey) throws IOException, GeneralSecurityException;
}