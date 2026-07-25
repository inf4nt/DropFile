package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
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
        validatePayloadTime("Session request", payload.timestamp());

        String key = getSessionRequestKey(payload);
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Session request reply detected. Rejected %s".formatted(key));
        }
    }

    public void tryToAddSessionResponse(HandshakeSessionDTO.SessionPayload payload) {
        validatePayloadTime("Session response", payload.timestamp());

        String key = getSessionResponseKey(payload);
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Session response reply detected. Rejected %s".formatted(key));
        }
    }

    public void tryToAddHandshakeRequest(HandshakeRequestDTO.Payload payload) {
        validatePayloadTime("Handshake request", payload.timestamp());

        String key = getHandshakeRequestKey(payload);
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Handshake request reply detected. Rejected %s".formatted(key));
        }
    }

    public void tryToAddHandshakeResponse(HandshakeResponseDTO.Payload payload) {
        validatePayloadTime("Handshake response", payload.timestamp());

        String key = getHandshakeResponseKey(payload);
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Handshake response reply detected. Rejected %s".formatted(key));
        }
    }

    public void tryToAddTunnelDispatcherRequest(String fingerprint, TunnelRequestDTO.Payload payload) {
        validatePayloadTime("Tunnel dispatcher request", payload.timestamp());

        String key = getTunnelDispatcherRequestKey(fingerprint, payload.requestId());
        Instant instant = requests.putIfAbsent(key, Instant.now());
        if (instant != null) {
            throw new RuntimeException("Tunnel dispatcher request reply detected. Rejected %s".formatted(key));
        }
    }

    public void validatePayloadTime(String operation, long timestamp) {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("%s payload timestamp must be greater than zero".formatted(operation));
        }
        long drift = Math.abs(System.currentTimeMillis() - timestamp);
        if (drift > TTL.toMillis()) {
            throw new RuntimeException("%s payload expired or clock drift too large".formatted(operation));
        }
    }

    @Override
    public void purge() {
        long cutoff = System.currentTimeMillis() - (TTL.toMillis() + 30_000);
        requests.values().removeIf(instant -> instant.toEpochMilli() < cutoff);
    }

    private String getTunnelDispatcherRequestKey(String fingerprint, String requestId) {
        return "t.req:" + fingerprint + ":" + requestId;
    }

    private String getHandshakeRequestKey(HandshakeRequestDTO.Payload payload) {
        return "h.req:" + CommonUtils
                .getFingerprint(
                        payload.publicKeyRSA(),
                        payload.publicKeyDH()
                );
    }

    private String getHandshakeResponseKey(HandshakeResponseDTO.Payload payload) {
        return "h.res:" + CommonUtils
                .getFingerprint(
                        payload.publicKeyRSA(),
                        payload.publicKeyDH()
                );
    }

    private String getSessionRequestKey(HandshakeSessionDTO.SessionPayload payload) {
        return "s.req:" + CommonUtils.getFingerprint(payload.publicKeyDH());
    }

    private String getSessionResponseKey(HandshakeSessionDTO.SessionPayload payload) {
        return "s.res:" + CommonUtils.getFingerprint(payload.publicKeyDH());
    }
}
