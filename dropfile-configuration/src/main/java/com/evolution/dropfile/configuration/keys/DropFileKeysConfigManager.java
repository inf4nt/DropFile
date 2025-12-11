package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.configuration.AbstractConfigManager;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class DropFileKeysConfigManager extends AbstractConfigManager {

    private static final String ALGORITHM = "RSA";

    private static final String KEYS_DIR_NAME = "keys";

    private static final String PUBLIC_KEY_FILENAME = "public.key";

    private static final String PRIVATE_KEY_FILENAME = "private.key";

    public KeyPair get() {
        KeyPair keyPair = readKeyPair();
        if (keyPair == null) {
            initConfigFiles();
            initDefaultConfigValues();
        }
        return readKeyPair();
    }

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
        byte[] keyByteArray = readPath(publicKeyFilePath);
        return KeyFactory.getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(keyByteArray));
    }

    @SneakyThrows
    private PrivateKey readPrivateKey() {
        Path privateKeyFilePath = resolvePrivateKeyFilePath();
        if (Files.notExists(privateKeyFilePath) || Files.size(privateKeyFilePath) == 0) {
            return null;
        }
        byte[] keyByteArray = readPath(privateKeyFilePath);

        return KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(keyByteArray));
    }

    @SneakyThrows
    private void initDefaultConfigValues() {
        KeyPair keyPair = CryptoUtils.generateKeyPair();
        writePath(resolvePublicKeyFilePath(), keyPair.getPublic().getEncoded());
        writePath(resolvePrivateKeyFilePath(), keyPair.getPrivate().getEncoded());
    }

    @SneakyThrows
    private void initConfigFiles() {
        Path publicKeyFilePath = resolvePublicKeyFilePath();
        Path privateKeyFilePath = resolvePrivateKeyFilePath();
        createFiles(publicKeyFilePath.toFile());
        createFiles(privateKeyFilePath.toFile());
    }

    private Path resolvePublicKeyFilePath() {
        return resolveKeysDirPath().resolve(PUBLIC_KEY_FILENAME);
    }

    private Path resolvePrivateKeyFilePath() {
        return resolveKeysDirPath().resolve(PRIVATE_KEY_FILENAME);
    }

    private Path resolveKeysDirPath() {
        return resolveProtectedHomeDirectory().resolve(KEYS_DIR_NAME);
    }
}
