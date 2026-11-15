package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeReadinessRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ReplyAttackGuard implements Purgeable {

    static final Duration PURGE_SAFETY_MARGIN = Duration.ofSeconds(30);

    private final Duration ttl;

    private final Duration maxFutureDrift;

    private final Duration retentionPeriod;

    private final Clock clock;

    private final Instant replayProtectionReadyAt;

    private final Map<String, Instant> requests = new ConcurrentHashMap<>();

    @Autowired
    public ReplyAttackGuard(DaemonApplicationProperties applicationProperties) {
        this(
                applicationProperties.daemonSecurityReplyTtl,
                applicationProperties.daemonSecurityReplyMaxFutureDriftTimeout,
                Clock.systemUTC()
        );
    }

    public ReplyAttackGuard(Duration ttl, Duration maxFutureDrift, Clock clock) {
        this.ttl = Objects.requireNonNull(ttl, "ttl must not be null");
        this.maxFutureDrift = Objects.requireNonNull(maxFutureDrift, "maxFutureDrift must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");

        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }

        if (maxFutureDrift.isNegative()) {
            throw new IllegalArgumentException("Max future drift must not be negative");
        }

        this.retentionPeriod = ttl.plus(maxFutureDrift).plus(PURGE_SAFETY_MARGIN);
        this.replayProtectionReadyAt = clock.instant().plus(maxFutureDrift);
    }

    public void sessionRequest(HandshakeSessionRequestDTO.Payload payload) {
        validatePayloadTime("Session request", payload.timestamp(), clock.instant());

        String key = getSessionRequestKey(payload);
        checkAndRegisterKey(key, "Session request");
    }

    public void readinessRequest(String fingerprint, HandshakeReadinessRequestDTO.Payload payload) {
        validatePayloadTime("Handshake readiness request", payload.timestamp(), clock.instant());

        String key = getHandshakeReadinessRequestKey(fingerprint, payload.requestId());
        checkAndRegisterKey(key, "Handshake readiness request");
    }

    public void handshakeRequest(HandshakeRequestDTO.Payload payload) {
        validatePayloadTime("Handshake request", payload.timestamp(), clock.instant());

        String key = getHandshakeRequestKey(payload);
        checkAndRegisterKey(key, "Handshake request");
    }

    public void tunnelDispatcherRequest(String fingerprint,
                                        TunnelRequestDTO.Payload payload) {
        validatePayloadTime("Tunnel dispatcher request", payload.timestamp(), clock.instant());

        String key = getTunnelDispatcherRequestKey(fingerprint, payload.requestId());
        checkAndRegisterKey(key, "Tunnel dispatcher request");
    }

    @Override
    public void purge() {
        Instant cutoff = clock.instant().minus(retentionPeriod);
        requests.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    private void checkAndRegisterKey(String key, String operation) {
        Instant existing = requests.putIfAbsent(key, clock.instant());
        if (existing != null) {
            throw new SecurityException("%s replay detected. Rejected %s".formatted(operation, key));
        }
    }

    private String getHandshakeReadinessRequestKey(String fingerprint, UUID requestId) {
        return "h.ready:" + Objects.requireNonNull(fingerprint) + ":" + Objects.requireNonNull(requestId);
    }

    private String getTunnelDispatcherRequestKey(String fingerprint, UUID requestId) {
        return "t.req:" + Objects.requireNonNull(fingerprint) + ":" + Objects.requireNonNull(requestId);
    }

    private String getHandshakeRequestKey(HandshakeRequestDTO.Payload payload) {
        return "h.req:" + Objects.requireNonNull(payload.requestId()) + ":" + CommonUtils.getFingerprint(
                Objects.requireNonNull(payload.publicKeyRSA()),
                Objects.requireNonNull(payload.publicKeyDH())
        );
    }

    private String getSessionRequestKey(HandshakeSessionRequestDTO.Payload payload) {
        return "s.req:" + Objects.requireNonNull(payload.requestId()) + ":" + CommonUtils.getFingerprint(payload.publicKeyDH());
    }

    public void validatePayloadTime(String operation, long timestamp, Instant now) {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("%s payload timestamp must be greater than zero".formatted(operation));
        }

        if (now.isBefore(replayProtectionReadyAt)) {
            throw new SecurityException("Replay protection is warming up. Please try again later.");
        }

        Instant payloadInstant = Instant.ofEpochMilli(timestamp);

        if (payloadInstant.isBefore(replayProtectionReadyAt)) {
            throw new SecurityException("%s payload timestamp is before replay protection ready threshold".formatted(operation));
        }

        Instant oldestAllowed = now.minus(ttl);
        if (payloadInstant.isBefore(oldestAllowed)) {
            throw new SecurityException("%s payload expired (past TTL)".formatted(operation));
        }

        Instant newestAllowed = now.plus(maxFutureDrift);
        if (payloadInstant.isAfter(newestAllowed)) {
            throw new SecurityException("%s payload timestamp too far in future (clock drift)".formatted(operation));
        }
    }
}
