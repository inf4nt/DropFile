package com.evolution.dropfile.common;

import com.evolution.dropfile.common.function.IORunnable;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CommonUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private static final String SHA256_ALGORITHM = "SHA-256";

    public static byte[] nonce(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static byte[] nonce12() {
        return nonce(12);
    }

    public static String random() {
        return UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10);
    }

    public static String generateRawSecretNonce12() {
        String secret;
        do {
            byte[] bytes = nonce12();
            secret = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);
        } while (secret.startsWith("-"));

        return secret;
    }

    public static URI toURI(String string) {
        if (!string.startsWith("http://") && !string.startsWith("https://")) {
            return URI.create("http://" + string);
        }
        return URI.create(string);
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
    public static String getFingerprint(byte[]... data) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        for (byte[] datum : data) {
            md.update(datum);
        }
        byte[] hash = md.digest();
        return HEX_FORMAT.formatHex(hash);
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
        } catch (Exception _) {
        }
    }

    @SafeVarargs
    public static boolean checkThrowable(Throwable throwable,
                                         Class<? extends Throwable>... lookFor) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable cause = throwable;

        while (cause != null && visited.add(cause)) {
            for (Class<? extends Throwable> targetClass : lookFor) {
                if (targetClass.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }

        return false;
    }

    public static <T> T requireOne(Collection<T> source) {
        return requireOne(source, null, null);
    }

    public static <T> T requireOne(Collection<T> source,
                                   Predicate<T> test) {
        return requireOne(source, test, null);
    }

    public static <T> T requireOne(Collection<T> source,
                                   Predicate<T> test,
                                   Supplier<String> prefixErrorMessageSupplier) {
        Stream<T> stream = source.stream();
        if (test != null) {
            stream = stream.filter(test);
        }
        List<T> elements = stream
                .toList();
        if (elements.isEmpty()) {
            String message = concatIfNotEmpty(
                    prefixErrorMessageSupplier,
                    "No items found"
            );
            throw new NoSuchElementException(message);
        }
        if (elements.size() != 1) {
            String message = concatIfNotEmpty(
                    prefixErrorMessageSupplier,
                    String.format("More than one item was found. Please provide more detailed criteria. Found: %s items", elements.size())
            );
            throw new IllegalStateException(message);
        }
        return elements.getFirst();
    }

    public static RuntimeException toRuntimeException(Throwable throwable) {
        return toRuntimeException(null, throwable);
    }

    public static RuntimeException toRuntimeException(String message, Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return message != null && !message.isBlank()
                    ? new RuntimeException(message, throwable)
                    : runtimeException;
        }

        if (throwable instanceof Error error) {
            throw message != null && !message.isBlank() ? new Error(message, error) : error;
        }

        return message != null && !message.isBlank()
                ? new RuntimeException(message, throwable)
                : new RuntimeException(throwable);
    }

    public static String joinPaths(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException();
        }

        List<String> validParts = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                validParts.add(part);
            }
        }

        if (validParts.isEmpty()) {
            return "";
        }

        String first = validParts.getFirst();
        String last = validParts.getLast();

        boolean startsWithSlash = first.startsWith("/");
        boolean endsWithSlash = last.endsWith("/");

        StringBuilder result = new StringBuilder();
        boolean hasContent = false;

        for (String part : validParts) {
            String stripped = part.replaceAll("^/+|/+$", "");
            if (!stripped.isEmpty()) {
                hasContent = true;
                if (!result.isEmpty()) {
                    result.append("/");
                }
                result.append(stripped);
            }
        }

        if (!hasContent) {
            return "";
        }

        if (startsWithSlash && !result.toString().startsWith("/")) {
            result.insert(0, "/");
        }

        if (endsWithSlash && !result.toString().endsWith("/")) {
            result.append("/");
        }

        return result.toString();
    }

    public static long getSize(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return Files.size(path);
        }

        AtomicLong totalSize = new AtomicLong(0);

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                totalSize.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return totalSize.get();
    }

    private static String concatIfNotEmpty(Supplier<String> prefixSupplier, String message) {
        if (prefixSupplier == null) {
            return message;
        }
        String prefix = prefixSupplier.get();
        if (prefix == null) {
            return message;
        }
        prefix = prefix.trim();
        if (prefix.isEmpty()) {
            return message;
        }
        return prefix + ". " + message;
    }
}
