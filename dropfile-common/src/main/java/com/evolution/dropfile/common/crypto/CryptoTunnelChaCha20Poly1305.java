package com.evolution.dropfile.common.crypto;

import com.evolution.dropfile.common.CommonUtils;
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
import java.util.Arrays;

public class CryptoTunnelChaCha20Poly1305 implements CryptoTunnel {

    private static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";

    private static final String SHA256_ALGORITHM = "SHA-256";

    private static final String SECRET_KEY_ALGORITHM = "ChaCha20";

    private static final int NONCE_LENGTH = 12;

    @Override
    public String getAlgorithm() {
        return CIPHER_ALGORITHM;
    }

    // TODO add HKDF
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
    public byte[] encryptInline(byte[] data, SecretKey key) {
        SecureEnvelope encrypt = encrypt(data, key);
        byte[] finalArray = new byte[encrypt.nonce().length + encrypt.payload().length];
        System.arraycopy(encrypt.nonce(), 0, finalArray, 0, encrypt.nonce().length);
        System.arraycopy(
                encrypt.payload(), 0, finalArray, encrypt.nonce().length, encrypt.payload().length
        );
        return finalArray;
    }

    @SneakyThrows
    @Override
    public SecureEnvelope encrypt(byte[] data, SecretKey key) {
        byte[] nonce = CommonUtils.nonce12();

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

    @Override
    public byte[] decryptInline(byte[] data, SecretKey key) {
        byte[] nonce = Arrays.copyOfRange(data, 0, NONCE_LENGTH);
        byte[] encrypt = Arrays.copyOfRange(data, NONCE_LENGTH, data.length);
        return decrypt(encrypt, nonce, key);
    }

    @SneakyThrows
    @Override
    public void encrypt(InputStream inputStream, OutputStream outputStream, SecretKey key) {
        try (CipherOutputStream cipherOut = encryptWrapper(outputStream, key)) {
            inputStream.transferTo(cipherOut);
        }
    }

    @SneakyThrows
    @Override
    public CipherOutputStream encryptWrapper(OutputStream outputStream, SecretKey key) {
        byte[] nonce = CommonUtils.nonce12();
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

        outputStream.write(nonce);
        return new CipherOutputStream(outputStream, cipher);
    }

    @SneakyThrows
    @Override
    public InputStream decrypt(InputStream inputStream, SecretKey key) {
        byte[] nonce = inputStream.readNBytes(NONCE_LENGTH);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));

        return new CipherInputStream(inputStream, cipher);
    }
}

