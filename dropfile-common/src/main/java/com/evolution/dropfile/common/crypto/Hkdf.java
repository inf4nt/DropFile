package com.evolution.dropfile.common.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Implementation of RFC 5869 HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
 * using HMAC-SHA256.
 *
 * <p>HKDF takes a raw, non-uniformly distributed secret (such as an ECDH shared secret)
 * and safely derives one or more cryptographically strong keys with domain separation.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5869">RFC 5869</a>
 */
public final class Hkdf {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final int PRK_LENGTH = 32;

    /**
     * HKDF-Extract Step (RFC 5869 Section 2.2).
     * <p>
     * Extracts a uniform Pseudorandom Key (PRK) from non-uniform raw secret material (IKM).
     *
     * @param salt      A 32-byte salt combining client and server nonces (clientNonce + serverNonce).
     *                  Using a unique salt per session prevents pre-computation attacks and binds
     *                  the derived key to this specific handshake exchange.
     * @param rawSecret The raw Input Keying Material (IKM), e.g., the raw bytes from ECDH agreement.
     *                  This value must not be empty.
     * @return A 32-byte Pseudorandom Key (PRK) containing concentrated entropy.
     * @throws NoSuchAlgorithmException if HMAC-SHA256 is not supported by the environment.
     * @throws InvalidKeyException      if the salt cannot initialize the Mac instance.
     */
    public static byte[] extract(byte[] salt, byte[] rawSecret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Objects.requireNonNull(salt, "Salt must not be null");
        Objects.requireNonNull(rawSecret, "Input Raw Secret (IKM) must not be null");

        if (salt.length != 32) {
            throw new IllegalArgumentException(
                    "Salt length must be exactly 32 bytes (clientNonce + serverNonce), got: " + salt.length
            );
        }

        if (rawSecret.length == 0) {
            throw new IllegalArgumentException("Raw Secret (IKM) must not be empty");
        }

        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(salt, HMAC_SHA256));
        return mac.doFinal(rawSecret);
    }

    /**
     * HKDF-Expand Step (RFC 5869 Section 2.3).
     * <p>
     * Expands the Pseudorandom Key (PRK) into a key of the requested length using
     * application-specific context info for domain separation.
     *
     * @param prk    The 32-byte Pseudorandom Key output from {@link #extract(byte[], byte[])}.
     * @param info   Context and application-specific information (e.g., "dropfile-v1-c2s").
     *               This enables Domain Separation: deriving completely distinct, independent
     *               keys for client-to-server, server-to-client, or storage needs from the same PRK.
     * @param length The target length of the derived key material in bytes (e.g., 32 for ChaCha20).
     * @return A byte array of the requested {@code length} containing the derived key material.
     * @throws NoSuchAlgorithmException if HMAC-SHA256 is not supported by the environment.
     * @throws InvalidKeyException      if PRK cannot initialize the Mac instance.
     */
    public static byte[] expand(byte[] prk, byte[] info, int length)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Objects.requireNonNull(prk, "PRK must not be null");

        if (prk.length != PRK_LENGTH) {
            throw new IllegalArgumentException(
                    "PRK length for HMAC-SHA256 must be exactly 32 bytes, got: " + prk.length
            );
        }

        if (length <= 0 || length > 255 * PRK_LENGTH) {
            throw new IllegalArgumentException("Invalid output key length: " + length);
        }

        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(prk, HMAC_SHA256));

        byte[] result = new byte[length];

        // RFC 5869: T(0) = empty string (zero length)
        byte[] t = new byte[0];
        int generated = 0;
        byte counter = 1;

        // Iterate T(i) = HMAC-Hash(PRK, T(i-1) | info | counter)
        while (generated < length) {
            mac.reset();
            mac.update(t); // Concatenate T(i-1)

            if (info != null && info.length > 0) {
                mac.update(info); // Concatenate domain label
            }

            mac.update(counter); // Concatenate 1-byte incremental counter
            t = mac.doFinal();

            // Copy generated block slice into the target array
            int todo = Math.min(t.length, length - generated);
            System.arraycopy(t, 0, result, generated, todo);
            generated += todo;
            counter++;
        }

        return result;
    }
}