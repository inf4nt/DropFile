package com.evolution.dropfile.common;


import lombok.SneakyThrows;

import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class CommonUtils {

    private static final String SHA256_ALGORITHM = "SHA256";

    public static String generateSecret() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);

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

    public static String random() {
        return UUID.randomUUID().toString()
                .replaceAll("-", "");
    }

    @SneakyThrows
    public static String digest(byte[] data) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = md.digest(data);
        return hexString(hash);
    }

    private static String hexString(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
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
}
