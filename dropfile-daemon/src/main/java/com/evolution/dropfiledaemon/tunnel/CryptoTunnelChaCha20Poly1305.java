package com.evolution.dropfiledaemon.tunnel;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class CryptoTunnelChaCha20Poly1305 implements CryptoTunnel {

    private static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";

    private static final String SHA256_ALGORITHM = "SHA-256";

    private static final String SECRET_KEY_ALGORITHM = "ChaCha20";

    private static final int NONCE_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String getAlgorithm() {
        return CIPHER_ALGORITHM;
    }

    @SneakyThrows
    @Override
    public SecretKey secretKey(byte[] secret) {
        byte[] digest = MessageDigest
                .getInstance(SHA256_ALGORITHM)
                .digest(secret);
        return new SecretKeySpec(digest, SECRET_KEY_ALGORITHM);
    }

    @SneakyThrows
    @Override
    public SecureEnvelope encrypt(byte[] data, SecretKey key) {
        byte[] nonce = generateNonce();

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

        byte[] encrypted = cipher.doFinal(data);
        return new SecureEnvelope(encrypted, nonce);
    }

    @SneakyThrows
    @Override
    public byte[] decrypt(byte[] payload, byte[] nonce, SecretKey key) {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));

        return cipher.doFinal(payload);
    }

    @SneakyThrows
    @Override
    public void encrypt(InputStream inputStream, OutputStream outputStream, SecretKey key) {
        byte[] nonce = generateNonce();
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

        outputStream.write(nonce);

        try (CipherOutputStream cipherOut = new CipherOutputStream(outputStream, cipher)) {
            inputStream.transferTo(cipherOut);
        }
    }

    @SneakyThrows
    @Override
    public InputStream decrypt(InputStream inputStream, SecretKey key) {
        byte[] nonce = inputStream.readNBytes(NONCE_LENGTH);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));

        return new CipherInputStream(inputStream, cipher);
    }

    private byte[] generateNonce() {
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        return nonce;
    }
}

