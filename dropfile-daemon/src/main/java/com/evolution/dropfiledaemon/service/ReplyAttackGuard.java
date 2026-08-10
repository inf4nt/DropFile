package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReplyAttackGuard implements Purgeable {

    private static final Duration TTL = Duration.ofSeconds(30);

    private static final Duration CLOCK_DRIFT_TOLERANCE = Duration.ofSeconds(10);

    private static final Duration RETENTION_PERIOD = TTL.multipliedBy(2).plus(CLOCK_DRIFT_TOLERANCE);

    private final Map<String, Instant> requests = new ConcurrentHashMap<>();

    public void tryToAddSessionRequest(HandshakeSessionDTO.SessionRequestPayload payload) {
        validatePayloadTime("Session request", payload.timestamp());

        String key = getSessionRequestKey(payload);
        checkAndRegisterKey(key, "Session request");
    }

    public void tryToAddHandshakeRequest(HandshakeRequestDTO.Payload payload) {
        validatePayloadTime("Handshake request", payload.timestamp());

        String key = getHandshakeRequestKey(payload);
        checkAndRegisterKey(key, "Handshake request");
    }

    public void tryToAddTunnelDispatcherRequest(String fingerprint, TunnelRequestDTO.
            Payload payload) {
        validatePayloadTime("Tunnel dispatcher request", payload.timestamp());

        String key = getTunnelDispatcherRequestKey(fingerprint, payload.requestId());
        checkAndRegisterKey(key, "Tunnel dispatcher request");
    }

    private void validatePayloadTime(String operation, long timestamp) {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("%s payload timestamp must be greater than zero".formatted(operation));
        }
        long drift = Math.abs(System.currentTimeMillis() - timestamp);
        if (drift > TTL.toMillis()) {
            throw new SecurityException("%s payload expired or clock drift too large".formatted(operation));
        }
    }

    @Override
    public void purge() {
        Instant cutoff = Instant.now().minus(RETENTION_PERIOD);
        requests.values().removeIf(insertedAt -> insertedAt.isBefore(cutoff));
    }

    private void checkAndRegisterKey(String key, String operation) {
        Instant existing = requests.putIfAbsent(key, Instant.now());
        if (existing != null) {
            throw new SecurityException("%s replay detected. Rejected %s".formatted(operation, key));
        }
    }

    private String getTunnelDispatcherRequestKey(String fingerprint, UUID requestId) {
        return "t.req:" + fingerprint + ":" + requestId;
    }

    private String getHandshakeRequestKey(HandshakeRequestDTO.Payload payload) {
        return "h.req:" + CommonUtils.getFingerprint(
                payload.publicKeyRSA(),
                payload.publicKeyDH()
        );
    }

    private String getSessionRequestKey(HandshakeSessionDTO.SessionRequestPayload payload) {
        return "s.req:" + CommonUtils.getFingerprint(payload.publicKeyDH());
    }
}