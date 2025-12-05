package com.evolution.dropfile.configuration.crypto;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    private static final String SHA256_WITH_RSA_ALGORITHM = "SHA256withRSA";

    private static final String RSA_ALGORITHM = "RSA";

    private static final String SHA256_ALGORITHM = "SHA256";

    @SneakyThrows
    public static PublicKey getPublicKey(byte[] publicKeyByteArray) {
        return KeyFactory
                .getInstance(RSA_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKeyByteArray));
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

    public static byte[] encrypt(PublicKey publicKey, byte[] payload) {
        return encrypt(publicKey.getEncoded(), payload);
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
    public static boolean verify(String data, String signature, byte[] publicKeyByteArray) {
        Signature sig = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        PublicKey publicKey = getPublicKey(publicKeyByteArray);
        sig.initVerify(publicKey);
        sig.update(data.getBytes());
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return sig.verify(signatureBytes);
    }

    @SneakyThrows
    public static String sign(String data, PrivateKey privateKey) {
        Signature signature = Signature.getInstance(SHA256_WITH_RSA_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
