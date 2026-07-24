package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class ReplyAttackGuard implements Purgeable {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final Map<String, Instant> requests = new ConcurrentHashMap<>();

    public void tryToAddSessionRequest(HandshakeSessionDTO.SessionPayload payload) {
        if (payload.timestamp() <= 0) {
            throw new IllegalArgumentException("Session payload timestamp must be greater than zero");
        }

        long drift = Math.abs(System.currentTimeMillis() - payload.timestamp());
        if (drift > TTL.toMillis()) {
            throw new RuntimeException("Session payload expired or clock drift too large");
        }

        String key = getSessionKey(payload);
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Session reply detected. Request rejected %s".formatted(key));
        }
    }

    public void tryToAddHandshakeRequest(HandshakeRequestDTO.Payload payload) {
        if (payload.timestamp() <= 0) {
            throw new IllegalArgumentException("Handshake payload timestamp must be greater than zero");
        }

        long drift = Math.abs(System.currentTimeMillis() - payload.timestamp());
        if (drift > TTL.toMillis()) {
            throw new RuntimeException("Handshake payload expired or clock drift too large");
        }

        String key = getHandshakeKey(payload);
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Handshake reply detected. Request rejected %s".formatted(key));
        }
    }

    public void tryToAddTunnelRequest(String fingerprint, TunnelRequestDTO.Payload payload) {
        if (payload.timestamp() <= 0) {
            throw new IllegalArgumentException("Tunnel payload timestamp must be greater than zero");
        }

        long drift = Math.abs(System.currentTimeMillis() - payload.timestamp());
        if (drift > TTL.toMillis()) {
            throw new RuntimeException("Tunnel payload expired or clock drift too large");
        }

        String key = getTunnelKey(fingerprint, payload.requestId());
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Tunnel reply detected. Request rejected %s".formatted(key));
        }
    }

    @Override
    public void purge() {
        long cutoff = System.currentTimeMillis() - (TTL.toMillis() + 30_000);
        requests.values().removeIf(instant -> instant.toEpochMilli() < cutoff);
    }

    private String getTunnelKey(String fingerprint, String requestId) {
        return "t:" + fingerprint + ":" + requestId;
    }

    private String getHandshakeKey(HandshakeRequestDTO.Payload payload) {
        return "h:" + CommonUtils
                .getFingerprint(
                        payload.publicKeyRSA(),
                        payload.publicKeyDH()
                );
    }

    private String getSessionKey(HandshakeSessionDTO.SessionPayload payload) {
        return "s:" + CommonUtils.getFingerprint(payload.publicKey());
    }
}
