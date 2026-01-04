package com.evolution.dropfile.common;


import lombok.SneakyThrows;

import java.net.URI;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class CommonUtils {

    private static final String SHA256_ALGORITHM = "SHA256";

    public static byte[] nonce16() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public static byte[] nonce12() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public static String random() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String generateSecret() {
        byte[] bytes = nonce16();
        String base64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return format(base64, 4, 4);
    }

    private static String format(String s, int groupSize, int groups) {
        s = s.replaceAll("-", "")
                .replaceAll("_", "");

        StringBuilder sb = new StringBuilder();
        int max = Math.min(s.length(), groupSize * groups);

        for (int i = 0; i < max; i++) {
            if (i > 0 && i % groupSize == 0) {
                sb.append('-');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
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
        return Base64.getEncoder().encodeToString(data);
    }

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

    private static String hexString(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
