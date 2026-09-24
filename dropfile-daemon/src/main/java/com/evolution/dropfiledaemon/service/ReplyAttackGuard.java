package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
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
                applicationProperties.daemonSecurityReplyMaxFutureDrift,
                Clock.systemUTC()
        );
    }

    public ReplyAttackGuard(Duration ttl, Duration maxFutureDrift, Clock clock) {
        this.ttl = Objects.requireNonNull(ttl);
        this.maxFutureDrift = Objects.requireNonNull(maxFutureDrift);
        this.clock = Objects.requireNonNull(clock);

        this.retentionPeriod = this.ttl.plus(this.maxFutureDrift).plus(PURGE_SAFETY_MARGIN);
        this.replayProtectionReadyAt = this.clock.instant().plus(this.maxFutureDrift);
    }

    public void sessionRequest(HandshakeSessionDTO.SessionRequestPayload payload) {
        validatePayloadTime("Session request", payload.timestamp(), clock.instant());

        String key = getSessionRequestKey(payload);
        checkAndRegisterKey(key, "Session request");
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

    private String getTunnelDispatcherRequestKey(String fingerprint, UUID requestId) {
        return "t.req:" + fingerprint + ":" + requestId;
    }

    private String getHandshakeRequestKey(HandshakeRequestDTO.Payload payload) {
        return "h.req:" + payload.requestId() + ":" + CommonUtils.getFingerprint(
                payload.publicKeyRSA(),
                payload.publicKeyDH()
        );
    }

    private String getSessionRequestKey(HandshakeSessionDTO.SessionRequestPayload payload) {
        return "s.req:" + payload.requestId() + ":" + CommonUtils.getFingerprint(payload.publicKeyDH());
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
