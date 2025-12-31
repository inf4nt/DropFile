package com.evolution.dropfile.common.crypto;

import lombok.SneakyThrows;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptoECDH {

    private static final String X25519_ALGORITHM = "X25519";

    private static final String SHA256_ALGORITHM = "SHA-256";

    private static final String SECRET_KEY_ALGORITHM = "ChaCha20";

    @SneakyThrows
    public static KeyPair generateKeyPair() {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(X25519_ALGORITHM);
        return kpg.generateKeyPair();
    }

    @SneakyThrows
    public static byte[] getSecretKey(PrivateKey privateKey, PublicKey publicKey) {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(X25519_ALGORITHM);
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    @SneakyThrows
    public static PublicKey getPublicKey(byte[] publicKey) {
        return KeyFactory.getInstance(X25519_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));
    }

    @SneakyThrows
    public static PrivateKey getPrivateKey(byte[] privateKey) {
        return KeyFactory.getInstance(X25519_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    }

}
