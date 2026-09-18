package com.evolution.dropfile.common.crypto;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptoECDH {

    private static final String X25519_ALGORITHM = "X25519";

    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(X25519_ALGORITHM);
        return kpg.generateKeyPair();
    }

    public static byte[] getSecretKey(PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(X25519_ALGORITHM);
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    public static PublicKey getPublicKey(byte[] publicKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(X25519_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));
    }

    public static PrivateKey getPrivateKey(byte[] privateKey) throws GeneralSecurityException {
        return KeyFactory.getInstance(X25519_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    }
}
