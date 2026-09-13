package com.evolution.dropfile.common.crypto;

import com.evolution.dropfile.common.CommonUtils;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

public class CryptoTunnelChaCha20Poly1305 implements CryptoTunnel {

    private static final int POLY1305_TAG_LENGTH = 16;

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
    public SecureEnvelope encrypt(byte[] data, SecretKey key) {
        byte[] nonce = CommonUtils.nonce12();
        if (nonce.length != NONCE_LENGTH) {
            throw new IOException("Invalid nonce length " + nonce.length);
        }

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
    public byte[] decrypt(InputStream inputStream, SecretKey key) {
        byte[] nonce = readNonce(inputStream);

        byte[] encryptedPayload = inputStream.readAllBytes();

        if (encryptedPayload.length < POLY1305_TAG_LENGTH) {
            throw new IOException("Premature EOF: stream too short for Poly1305 MAC tag");
        }

        return decrypt(encryptedPayload, nonce, key);
    }

    @SneakyThrows
    @Override
    public CipherOutputStream encryptWrapper(OutputStream outputStream, SecretKey key) {
        byte[] nonce = CommonUtils.nonce12();
        if (nonce.length != NONCE_LENGTH) {
            throw new IOException("Invalid nonce length " + nonce.length);
        }

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

        outputStream.write(nonce);
        return new CipherOutputStream(outputStream, cipher);
    }

    @SneakyThrows
    @Override
    public InputStream decryptStreaming(InputStream inputStream, SecretKey key) {
        byte[] nonce = readNonce(inputStream);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));

        return new CipherInputStream(inputStream, cipher);
    }

    @Override
    public byte[] readNonce(InputStream inputStream) throws IOException {
        byte[] nonce = inputStream.readNBytes(NONCE_LENGTH);
        if (nonce.length != NONCE_LENGTH) {
            throw new IOException("Premature EOF: incomplete nonce in stream");
        }
        return nonce;
    }
}
