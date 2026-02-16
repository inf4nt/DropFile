package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.HandshakeApiTrustInResponseDTO;
import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import jakarta.annotation.Nullable;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

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

    public static List<HandshakeApiTrustOutResponseDTO> mapToHandshakeApiTrustOutResponseDTOList(Map<String, HandshakeTrustedOutStore.TrustedOut> trusts,
                                                                                                 Map<String, HandshakeSessionStore.SessionValue> sessions) {
        return trusts.entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    HandshakeSessionStore.SessionValue sessionValue = sessions.get(fingerprint);
                    return mapToHandshakeApiTrustOutResponseDTO(fingerprint, entry.getValue(), sessionValue);
                })
                .toList();
    }

    public static HandshakeApiTrustOutResponseDTO mapToHandshakeApiTrustOutResponseDTO(String fingerprint,
                                                                                       HandshakeTrustedOutStore.TrustedOut trustedOut,
                                                                                       @Nullable HandshakeSessionStore.SessionValue sessionValue) {
        return new HandshakeApiTrustOutResponseDTO(
                fingerprint,
                CommonUtils.encodeBase64(trustedOut.publicRSA()),
                CommonUtils.encodeBase64(trustedOut.remoteRSA()),
                sessionValue == null ? null : CommonUtils.encodeBase64(sessionValue.publicDH()),
                sessionValue == null ? null : CommonUtils.encodeBase64(sessionValue.remotePublicDH()),
                trustedOut.addressURI().toString(),
                trustedOut.created(),
                sessionValue == null ? null : sessionValue.updated()
        );
    }

    public static List<HandshakeApiTrustInResponseDTO> mapToHandshakeApiTrustInResponseDTOList(Map<String, HandshakeTrustedInStore.TrustedIn> trusts,
                                                                                               Map<String, HandshakeSessionStore.SessionValue> sessions) {
        return trusts.entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    HandshakeSessionStore.SessionValue sessionValue = sessions.get(fingerprint);
                    return mapToHandshakeApiTrustInResponseDTO(fingerprint, entry.getValue(), sessionValue);
                })
                .toList();
    }

    public static HandshakeApiTrustInResponseDTO mapToHandshakeApiTrustInResponseDTO(String fingerprint,
                                                                                      HandshakeTrustedInStore.TrustedIn trustedIn,
                                                                                      @Nullable HandshakeSessionStore.SessionValue sessionValue) {
        return new HandshakeApiTrustInResponseDTO(
                fingerprint,
                CommonUtils.encodeBase64(trustedIn.publicRSA()),
                CommonUtils.encodeBase64(trustedIn.remoteRSA()),
                sessionValue == null ? null : CommonUtils.encodeBase64(sessionValue.publicDH()),
                sessionValue == null ? null : CommonUtils.encodeBase64(sessionValue.remotePublicDH()),
                trustedIn.created(),
                sessionValue == null ? null : sessionValue.updated()
        );
    }
}
