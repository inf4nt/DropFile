package com.evolution.dropfile.common.crypto;

import lombok.SneakyThrows;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptoRSA {

    private static final String SHA256_WITH_RSA_ALGORITHM = "SHA256withRSA";

    private static final String RSA_ALGORITHM = "RSA";

    @SneakyThrows
    public static KeyPair generateKeyPair() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @SneakyThrows
    public static PublicKey getPublicKey(byte[] publicKey) {
        return KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));
    }

    @SneakyThrows
    public static PrivateKey getPrivateKey(byte[] privateKey) {
        return KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    }

    @SneakyThrows
    public static byte[] sign(byte[] data, PrivateKey privateKey) {
        Signature signature = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    @SneakyThrows
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        Signature sig = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }
}
