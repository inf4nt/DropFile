package com.evolution.dropfile.common.crypto;

import lombok.SneakyThrows;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Base64;

public class CryptoUtils {

    private static final String SHA256_ALGORITHM = "SHA256";

    @SneakyThrows
    public static String getFingerprint(byte[] data) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = md.digest(data);
        return hexString(hash);
    }

    @SneakyThrows
    public static String getFingerprint(PublicKey publicKey) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = md.digest(publicKey.getEncoded());
        return hexString(hash);
    }

    public static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static String hexString(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
