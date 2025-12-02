package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.AbstractProtectedConfigManager;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public class DropFileKeysConfigManager extends AbstractProtectedConfigManager {

    private static final String KEYS_DIR_NAME = "keys";

    private static final String PUBLIC_KEY_FILENAME = "public.key";

    private static final String PRIVATE_KEY_FILENAME = "private.key";

    private KeyPair keyPair;

    @SneakyThrows
    public KeyPair getKeyPair() {
        if (keyPair != null) {
            return keyPair;
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        this.keyPair = pair;

        return pair;
    }

//    public KeyPair getKeyPair() {
//        KeyPair keyPair = readKeyPair();
//        if (keyPair == null) {
//            initConfigFiles();
//            initDefaultConfigValues();
//        }
//        keyPair = readKeyPair();
//        Objects.requireNonNull(keyPair);
//        return keyPair;
//    }

    @SneakyThrows
    private KeyPair readKeyPair() {
        PublicKey publicKey = readPublicKey();
        if (publicKey == null) {
            return null;
        }
        PrivateKey privateKey = readPrivateKey();
        if (privateKey == null) {
            return null;
        }
        return new KeyPair(publicKey, privateKey);
    }

    @SneakyThrows
    private PublicKey readPublicKey() {
        Path publicKeyFilePath = resolvePublicKeyFilePath();
        if (Files.notExists(publicKeyFilePath) || Files.size(publicKeyFilePath) == 0) {
            return null;
        }
        byte[] keyByteArray = Files.readAllBytes(publicKeyFilePath);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyByteArray));
    }

    @SneakyThrows
    private PrivateKey readPrivateKey() {
        Path privateKeyFilePath = resolvePrivateKeyFilePath();
        if (Files.notExists(privateKeyFilePath) || Files.size(privateKeyFilePath) == 0) {
            return null;
        }
        byte[] keyByteArray = Files.readAllBytes(privateKeyFilePath);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyByteArray));
    }

    @SneakyThrows
    public void initDefaultConfigValues() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        Files.write(resolvePublicKeyFilePath(), pair.getPublic().getEncoded());
        Files.write(resolvePrivateKeyFilePath(), pair.getPrivate().getEncoded());
    }

    @SneakyThrows
    public void initConfigFiles() {
        Path keysDirPath = resolveKeysDirPath();
        if (Files.notExists(keysDirPath)) {
            Files.createDirectory(keysDirPath);
        }
        Path publicKeyFilePath = resolvePublicKeyFilePath();
        if (Files.notExists(publicKeyFilePath)) {
            Files.createFile(publicKeyFilePath);
        }
        Path privateKeyFilePath = resolvePrivateKeyFilePath();
        if (Files.notExists(privateKeyFilePath)) {
            Files.createFile(privateKeyFilePath);
        }
    }

    private Path resolvePublicKeyFilePath() {
        return resolveKeysDirPath().resolve(PUBLIC_KEY_FILENAME);
    }

    private Path resolvePrivateKeyFilePath() {
        return resolveKeysDirPath().resolve(PRIVATE_KEY_FILENAME);
    }

    private Path resolveKeysDirPath() {
        return resolveHomePath().resolve(KEYS_DIR_NAME);
    }
}
