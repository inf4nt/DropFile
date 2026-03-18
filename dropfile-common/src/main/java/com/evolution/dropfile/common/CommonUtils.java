package com.evolution.dropfile.common;


import com.evolution.dropfile.common.function.IORunnable;
import lombok.SneakyThrows;

import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
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
        return HexFormat.of().formatHex(hash);
    }

    @SneakyThrows
    public static void isInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    @SneakyThrows
    public static void isInterrupted(String message) {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException(message);
        }
    }

    public static String percent(long total, long downloaded) {
        if (total == 0) {
            return "0%";
        }
        if (downloaded == 0) {
            return "0%";
        }
        if (total == downloaded) {
            return "100%";
        }

        double value = (double) (downloaded * 100) / total;
        return String.format(Locale.US, "%.2f%%", value);
    }

    public static String toDisplaySize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative");
        }
        if (size == 0) {
            return "0B";
        }
        if (size < 1024) {
            return String.format("%sB", size);
        }
        if (size < 1024 * 1024) {
            double kb = size / 1024D;
            return String.format(Locale.US, "%.2fKB", kb);
        }
        if (size < 1024 * 1024 * 1024) {
            double mb = size / (1024 * 1024D);
            return String.format(Locale.US, "%.2fMB", mb);
        }
        double gb = size / (1024 * 1024 * 1024D);
        return String.format(Locale.US, "%.2fGB", gb);
    }

    public static void executeSafety(IORunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
