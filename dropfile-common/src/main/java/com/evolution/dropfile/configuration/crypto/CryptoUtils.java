package com.evolution.dropfile.configuration.crypto;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class CryptoUtils {

    private static final String ALGORITHM = "RSA";

    private static final String SHA256_ALGORITHM = "SHA256";

    @SneakyThrows
    public static PublicKey getPublicKey(byte[] publicKeyByteArray) {
        return KeyFactory
                .getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKeyByteArray));
    }

    @SneakyThrows
    public static String getFingerPrint(PublicKey publicKey) {
        return getFingerPrint(publicKey.getEncoded());
    }

    @SneakyThrows
    public static String getFingerPrint(byte[] publicKeyBytes) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = md.digest(publicKeyBytes);
        return hexString(hash);
    }

    public static byte[] encrypt(PublicKey publicKey, byte[] payload) {
        return encrypt(publicKey.getEncoded(), payload);
    }

    @SneakyThrows
    public static byte[] encrypt(byte[] publicKeyByteArray, byte[] payload) {
        PublicKey publicKey = KeyFactory
                .getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKeyByteArray));
        Cipher encryptCipher = Cipher.getInstance(ALGORITHM);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return encryptCipher.doFinal(payload);
    }

    @SneakyThrows
    public static byte[] decrypt(PrivateKey privateKey, byte[] encryptedMessageBytes) {
        Cipher decryptCipher = Cipher.getInstance(ALGORITHM);
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return decryptCipher.doFinal(encryptedMessageBytes);
    }

    public static String hexString(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
