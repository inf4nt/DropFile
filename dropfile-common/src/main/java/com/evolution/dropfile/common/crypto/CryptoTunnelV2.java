package com.evolution.dropfile.common.crypto;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public interface CryptoTunnelV2 {

    String getAlgorithm();

    SecretKey secretKey(byte[] rawSecret, String info, byte[] salt) throws GeneralSecurityException;

    SecureEnvelope encrypt(byte[] data, byte[] aad, SecretKey secretKey) throws GeneralSecurityException;

    byte[] decrypt(byte[] payload, byte[] nonce, byte[] aad, SecretKey secretKey) throws GeneralSecurityException;

    byte[] decrypt(InputStream inputStream, byte[] aad, SecretKey secretKey) throws GeneralSecurityException, IOException;
}
