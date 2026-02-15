package com.evolution.dropfile.common;


import lombok.SneakyThrows;

import java.net.URI;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class CommonUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String SHA256_ALGORITHM = "SHA-256";

    public static byte[] nonce12() {
        byte[] bytes = new byte[12];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String random() {
        return UUID.randomUUID().toString()
                .replaceAll("-", "")
                .substring(0, 10);
    }

    public static String generateSecretNonce12() {
        byte[] bytes = nonce12();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static URI toURI(String host) {
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            return URI.create("http://" + host);
        }
        return URI.create(host);
    }

    public static URI toURI(String host, Integer port) {
        if (port == null) {
            return toURI(host);
        }
        return toURI(host + ":" + port);
    }

    public static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().withoutPadding().encodeToString(data);
    }

    @SneakyThrows
    public static String getFingerprint(byte[] data) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = md.digest(data);
        return hexString(hash);
    }

    @SneakyThrows
    public static String getFingerprint(PublicKey publicKey) {
        return getFingerprint(publicKey.getEncoded());
    }

    private static String hexString(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
