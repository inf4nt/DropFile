package com.evolution.dropfile.common.crypto;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class CryptoTunnelChaCha20Poly1305 implements CryptoTunnel {

    private static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";

    private static final String SHA256_ALGORITHM = "SHA-256";

    private static final String SECRET_KEY_ALGORITHM = "ChaCha20";

    private static final int NONCE_LENGTH = 12;

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

    private static byte[] generateNonce() {
        byte[] nonce = new byte[NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }
}

