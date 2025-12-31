package com.evolution.dropfile.common.crypto;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;

public class CryptoTunnelChaCha {

    private static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";

    private static final int NONCE_LENGTH = 12;

    public record SecureEnvelope(byte[] payload, byte[] nonce) {
    }

    @SneakyThrows
    public static SecureEnvelope encrypt(byte[] data, SecretKey key) {
        byte[] nonce = generateNonce();

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

        byte[] encrypted = cipher.doFinal(data);
        return new SecureEnvelope(encrypted, nonce);
    }

    @SneakyThrows
    public static byte[] decrypt(SecureEnvelope envelope, SecretKey key) {
        return decrypt(envelope.payload(), envelope.nonce(), key);
    }

    @SneakyThrows
    public static byte[] decrypt(byte[] payload, byte[] nonce, SecretKey key) {
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

