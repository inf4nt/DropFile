package com.evolution.dropfile.common.crypto;

import com.evolution.dropfile.common.CommonUtils;
import jakarta.annotation.Nullable;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

public class CryptoTunnelV2Impl implements CryptoTunnelV2 {

    private static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";

    private static final String SECRET_KEY_ALGORITHM = "ChaCha20";

    private static final int NONCE_LENGTH = 12;

    private static final int SECRET_KEY_LENGTH = 32;

    @Override
    public String getAlgorithm() {
        return CIPHER_ALGORITHM;
    }

    @Override
    public SecretKey secretKey(byte[] derivedKey) {
        Objects.requireNonNull(derivedKey, "Derived key must not be null");
        return new SecretKeySpec(derivedKey, SECRET_KEY_ALGORITHM);
    }

    @Override
    public byte[] deriveKeyHkdf(byte[] rawSecret, String info, @Nullable byte[] salt) throws GeneralSecurityException {
        Objects.requireNonNull(rawSecret, "Raw secret must not be null");
        Objects.requireNonNull(info, "Info label must not be null");

        byte[] prk = Hkdf.extract(rawSecret, salt);
        return Hkdf.expand(prk, info.getBytes(StandardCharsets.UTF_8), SECRET_KEY_LENGTH);
    }

    @Override
    public SecureEnvelope encrypt(byte[] payload, byte[] aad, SecretKey secretKey) throws GeneralSecurityException {
        Objects.requireNonNull(payload, "Payload must not be null");
        Objects.requireNonNull(secretKey, "SecretKey must not be null");
        validateAad(aad);

        byte[] nonce = CommonUtils.nonce12();
        validateNonce(nonce);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(nonce));
        cipher.updateAAD(aad);

        byte[] encrypted = cipher.doFinal(payload);
        return new SecureEnvelope(encrypted, nonce);
    }

    @Override
    public byte[] decrypt(byte[] payload, byte[] nonce, byte[] aad, SecretKey secretKey) throws GeneralSecurityException {
        Objects.requireNonNull(payload, "Payload must not be null");
        Objects.requireNonNull(secretKey, "SecretKey must not be null");
        validateAad(aad);
        validateNonce(nonce);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(nonce));
        cipher.updateAAD(aad);

        return cipher.doFinal(payload);
    }

    @Override
    public byte[] decrypt(InputStream inputStream, byte[] aad, SecretKey secretKey) throws GeneralSecurityException, IOException {
        Objects.requireNonNull(inputStream, "InputStream must not be null");
        Objects.requireNonNull(secretKey, "SecretKey must not be null");
        validateAad(aad);

        byte[] nonce = inputStream.readNBytes(NONCE_LENGTH);
        validateNonce(nonce);

        byte[] payload = inputStream.readAllBytes();
        return decrypt(payload, nonce, aad, secretKey);
    }

    @Override
    public OutputStream encryptWrapper(OutputStream outputStream, byte[] aad, SecretKey secretKey) throws GeneralSecurityException, IOException {
        Objects.requireNonNull(outputStream, "OutputStream must not be null");
        Objects.requireNonNull(secretKey, "SecretKey must not be null");
        validateAad(aad);

        byte[] nonce = CommonUtils.nonce12();
        validateNonce(nonce);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(nonce));
        cipher.updateAAD(aad);

        outputStream.write(nonce);
        return new CipherOutputStream(outputStream, cipher);
    }

    @Override
    public InputStream decryptStreaming(InputStream inputStream, byte[] aad, SecretKey secretKey) throws GeneralSecurityException, IOException {
        Objects.requireNonNull(inputStream, "InputStream must not be null");
        Objects.requireNonNull(secretKey, "SecretKey must not be null");
        validateAad(aad);

        byte[] nonce = inputStream.readNBytes(NONCE_LENGTH);
        validateNonce(nonce);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(nonce));
        cipher.updateAAD(aad);

        return new CipherInputStream(inputStream, cipher);
    }

    private void validateNonce(byte[] nonce) {
        Objects.requireNonNull(nonce, "Nonce must not be null");
        if (nonce.length != NONCE_LENGTH) {
            throw new IllegalArgumentException("Invalid nonce length: " + nonce.length + " (expected " + NONCE_LENGTH + ")");
        }
    }

    private void validateAad(byte[] aad) {
        Objects.requireNonNull(aad, "AAD must not be null");
        if (aad.length == 0) {
            throw new IllegalArgumentException("AAD must not be empty");
        }
    }
}