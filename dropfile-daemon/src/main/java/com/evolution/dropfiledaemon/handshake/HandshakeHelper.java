package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

@Component
public class HandshakeHelper {

    private final int handshakeServerPayloadLiveTime;

    @Autowired
    public HandshakeHelper(DaemonApplicationProperties daemonApplicationProperties) {
        this.handshakeServerPayloadLiveTime = daemonApplicationProperties.daemonHandshakeServerPayloadLiveTime;
    }

    public void validateHandshakeLiveTimeout(Long timestamp) {
        if (timestamp == null) {
            throw new NullPointerException("timestamp is null");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > handshakeServerPayloadLiveTime) {
            throw new RuntimeException("Timed out");
        }
    }

    public void matchFingerprint(String fingerprint, PublicKey publicKey) {
        String fingerprintExpected = CommonUtils.getFingerprint(publicKey.getEncoded());
        if (!fingerprintExpected.equals(fingerprint)) {
            throw new IllegalStateException(String.format(
                    "Fingerprint mismatch %s vs %s", fingerprint, fingerprintExpected
            ));
        }
    }
}
