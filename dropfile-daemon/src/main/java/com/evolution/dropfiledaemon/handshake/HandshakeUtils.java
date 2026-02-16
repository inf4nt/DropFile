package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;

import java.security.PublicKey;

public class HandshakeUtils {

    private static final int MAX_HANDSHAKE_PAYLOAD_LIVE_TIMEOUT = 30_000;

    public static void validateHandshakeLiveTimeout(Long timestamp) {
        if (timestamp == null) {
            throw new NullPointerException("timestamp is null");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_HANDSHAKE_PAYLOAD_LIVE_TIMEOUT) {
            throw new RuntimeException("Timed out");
        }
    }

    public static void matchFingerprint(String fingerprint, PublicKey publicKey) {
        String fingerprintExpected = CommonUtils.getFingerprint(publicKey.getEncoded());
        if (!fingerprintExpected.equals(fingerprint)) {
            throw new IllegalStateException(String.format(
                    "Fingerprint mismatch %s vs %s", fingerprint, fingerprintExpected
            ));
        }
    }
}
