package com.evolution.dropfile.common.crypto;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    private static final String SHA256_WITH_RSA_ALGORITHM = "SHA256withRSA";

    private static final String RSA_ALGORITHM = "RSA";

    private static final String SHA256_ALGORITHM = "SHA256";

    public static KeyPair toKeyPair(byte[] publicKey, byte[] privateKey) {
        return new KeyPair(
                getPublicKey(publicKey),
                getPrivateKey(privateKey)
        );
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
    public static String getFingerPrint(PublicKey publicKey) {
        return getFingerPrint(publicKey.getEncoded());
    }

    @SneakyThrows
    public static String getFingerPrint(byte[] publicKeyBytes) {
        MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = md.digest(publicKeyBytes);
        return hexString(hash);
    }

    @SneakyThrows
    public static KeyPair generateKeyPair() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @SneakyThrows
    public static byte[] encrypt(byte[] publicKeyByteArray, byte[] payload) {
        PublicKey publicKey = KeyFactory
                .getInstance(RSA_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKeyByteArray));
        Cipher encryptCipher = Cipher.getInstance(RSA_ALGORITHM);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return encryptCipher.doFinal(payload);
    }

    @SneakyThrows
    public static byte[] decrypt(byte[] privateKeyByteArray, byte[] encryptedMessageBytes) {
        Cipher decryptCipher = Cipher.getInstance(RSA_ALGORITHM);
        PrivateKey privateKey = CryptoUtils.getPrivateKey(privateKeyByteArray);
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return decryptCipher.doFinal(encryptedMessageBytes);
    }

    @SneakyThrows
    public static byte[] decrypt(PrivateKey privateKey, byte[] encryptedMessageBytes) {
        Cipher decryptCipher = Cipher.getInstance(RSA_ALGORITHM);
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return decryptCipher.doFinal(encryptedMessageBytes);
    }

    public static String hexString(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @SneakyThrows
    public static boolean verify(byte[] data, byte[] signature, byte[] publicKeyByteArray) {
        Signature sig = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        PublicKey publicKey = getPublicKey(publicKeyByteArray);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }

    @SneakyThrows
    public static byte[] sign(String data, PrivateKey privateKey) {
        Signature signature = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return signature.sign();
    }

    @SneakyThrows
    public static byte[] sign(String data, byte[] privateKeyByteArray) {
        Signature signature = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        PrivateKey privateKey = getPrivateKey(privateKeyByteArray);
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return signature.sign();
    }

    public static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
