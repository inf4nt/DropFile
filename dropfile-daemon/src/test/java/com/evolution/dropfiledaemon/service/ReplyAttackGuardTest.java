package com.evolution.dropfiledaemon.service;

import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReplyAttackGuardTest {

    private static final Instant START_TIME = Instant.parse("2026-01-01T10:00:00Z");

    private static final Duration TTL = Duration.ofSeconds(30);

    private static final Duration MAX_FUTURE_DRIFT = Duration.ofSeconds(10);

    private TestClock testClock;

    private ReplyAttackGuard guard;

    @BeforeEach
    void setUp() {
        testClock = new TestClock(START_TIME);
        guard = new ReplyAttackGuard(TTL, MAX_FUTURE_DRIFT, testClock);
    }

    @Test
    void validatePayloadTime_ShouldThrow_WhenTimestampIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> guard.validatePayloadTime("test", 0, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldThrow_WhenTimestampIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> guard.validatePayloadTime("test", -500, testClock.instant()));
    }


    @Test
    void validatePayloadTime_ShouldThrowWarmupException_WhenInWarmupWindow() {
        testClock.plus(MAX_FUTURE_DRIFT.dividedBy(2));

        long currentTimestamp = testClock.millis();

        assertThrows(
                SecurityException.class,
                () -> guard.validatePayloadTime("test", currentTimestamp, testClock.instant())
        );
    }

    @Test
    void validatePayloadTime_ShouldThrowWarmupException_OneMsBeforeWarmupEnds() {
        testClock.plus(MAX_FUTURE_DRIFT).minusMillis(1);

        long currentTimestamp = testClock.millis();

        assertThrows(
                SecurityException.class,
                () -> guard.validatePayloadTime("test", currentTimestamp, testClock.instant())
        );
    }

    @Test
    void validatePayloadTime_ShouldAccept_WhenNowIsAtExactBootstrapThresholdBoundary() {
        testClock.plus(MAX_FUTURE_DRIFT);

        long exactBoundaryTimestamp = START_TIME.plus(MAX_FUTURE_DRIFT).toEpochMilli();

        assertDoesNotThrow(() -> guard.validatePayloadTime("test", exactBoundaryTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldReject_WhenTimestampIsExactlyOneMsBeforeBootstrapThreshold() {
        testClock.plus(MAX_FUTURE_DRIFT);

        long bootstrapThresholdMs = START_TIME.plus(MAX_FUTURE_DRIFT).toEpochMilli();

        assertThrows(SecurityException.class,
                () -> guard.validatePayloadTime("test", bootstrapThresholdMs - 1, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldAccept_WhenTimestampIsOneMsAfterBootstrapThresholdBoundary() {
        advanceClockPastBootstrap();
        long thresholdPlusOneMs = START_TIME.plus(MAX_FUTURE_DRIFT).toEpochMilli() + 1;
        assertDoesNotThrow(() -> guard.validatePayloadTime("test", thresholdPlusOneMs, testClock.instant()));
    }


    @Test
    void validatePayloadTime_ShouldAccept_WhenTimestampIsFromValidPast() {
        testClock.plus(MAX_FUTURE_DRIFT).plus(TTL);

        long bootstrapMs = START_TIME.plus(MAX_FUTURE_DRIFT).toEpochMilli();
        long nowMs = testClock.millis();
        long validPastTimestamp = bootstrapMs + (nowMs - bootstrapMs) / 2;

        assertDoesNotThrow(() -> guard.validatePayloadTime("test", validPastTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldAccept_WhenTimestampIsAtCurrentTime() {
        advanceClockPastBootstrap();
        assertDoesNotThrow(() -> guard.validatePayloadTime("test", testClock.millis(), testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldAccept_WhenTimestampIsFromValidFuture() {
        advanceClockPastBootstrap();
        long validFutureTimestamp = testClock.instant().plus(MAX_FUTURE_DRIFT.dividedBy(2)).toEpochMilli();
        assertDoesNotThrow(() -> guard.validatePayloadTime("test", validFutureTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldReject_WhenPastDriftExceedsTtl() {
        testClock.plus(MAX_FUTURE_DRIFT).plus(TTL).plusSeconds(2);
        long expiredTimestamp = testClock.instant().minus(TTL).minusMillis(1).toEpochMilli();
        assertThrows(SecurityException.class,
                () -> guard.validatePayloadTime("test", expiredTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldAccept_WhenPastDriftIsAtExactTtlBoundary() {
        testClock.plus(MAX_FUTURE_DRIFT).plus(TTL).plusSeconds(2);
        long exactTtlBoundaryTimestamp = testClock.instant().minus(TTL).toEpochMilli();
        assertDoesNotThrow(() -> guard.validatePayloadTime("test", exactTtlBoundaryTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldReject_WhenFutureDriftExceedsLimit() {
        advanceClockPastBootstrap();
        long tooFarFutureTimestamp = testClock.instant().plus(MAX_FUTURE_DRIFT).plusMillis(1).toEpochMilli();
        assertThrows(SecurityException.class,
                () -> guard.validatePayloadTime("test", tooFarFutureTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldAccept_WhenFutureDriftIsAtExactLimitBoundary() {
        advanceClockPastBootstrap();
        long exactFutureBoundaryTimestamp = testClock.instant().plus(MAX_FUTURE_DRIFT).toEpochMilli();
        assertDoesNotThrow(() -> guard.validatePayloadTime("test", exactFutureBoundaryTimestamp, testClock.instant()));
    }

    @Test
    void validatePayloadTime_ShouldReject_WhenTimestampIsBeforeBootstrapThreshold() {
        advanceClockPastBootstrap();
        long timestampBeforeThreshold = START_TIME.plus(MAX_FUTURE_DRIFT).minusMillis(1).toEpochMilli();
        assertThrows(SecurityException.class,
                () -> guard.validatePayloadTime("test", timestampBeforeThreshold, testClock.instant()));
    }


    @Test
    void shouldRejectMessageCreatedBeforeRestartEvenAfterBootstrapEnds() {
        advanceClockPastBootstrap();
        testClock.plus(TTL);

        long oldTimestamp = START_TIME.plus(MAX_FUTURE_DRIFT).minusMillis(1).toEpochMilli();

        assertThrows(
                SecurityException.class,
                () -> guard.tunnelDispatcherRequest("fp", createTunnelPayload(UUID.randomUUID(), oldTimestamp))
        );
    }


    @Test
    void shouldAcceptValidTunnelRequest() {
        advanceClockPastBootstrap();
        long validTimestamp = testClock.millis();
        assertDoesNotThrow(() -> guard.tunnelDispatcherRequest("fp", createTunnelPayload(UUID.randomUUID(), validTimestamp)));
    }

    @Test
    void shouldDetectReplayAttackWithSameKey() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID requestId = UUID.randomUUID();

        guard.tunnelDispatcherRequest("fp123", createTunnelPayload(requestId, timestamp));

        assertThrows(
                SecurityException.class,
                () -> guard.tunnelDispatcherRequest("fp123", createTunnelPayload(requestId, timestamp))
        );
    }

    @Test
    void shouldAllowSameRequestIdForDifferentFingerprints() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID requestId = UUID.randomUUID();

        assertDoesNotThrow(() -> guard.tunnelDispatcherRequest("fp1", createTunnelPayload(requestId, timestamp)));
        assertDoesNotThrow(() -> guard.tunnelDispatcherRequest("fp2", createTunnelPayload(requestId, timestamp)));
    }

    @Test
    void shouldAllowDifferentRequestIdsForSameFingerprint() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();

        assertDoesNotThrow(() -> guard.tunnelDispatcherRequest("fp1", createTunnelPayload(UUID.randomUUID(), timestamp)));
        assertDoesNotThrow(() -> guard.tunnelDispatcherRequest("fp1", createTunnelPayload(UUID.randomUUID(), timestamp)));
    }

    @Test
    void shouldRejectReplayUntilPurgeRunsEvenIfTtlExpired() {
        advanceClockPastBootstrap();
        UUID requestId = UUID.randomUUID();
        long ts = testClock.millis();

        guard.tunnelDispatcherRequest("fp", createTunnelPayload(requestId, ts));

        testClock.plus(TTL).plusSeconds(2);

        assertThrows(
                SecurityException.class,
                () -> guard.tunnelDispatcherRequest("fp", createTunnelPayload(requestId, testClock.millis()))
        );
    }

    @Test
    void shouldPurgeExpiredKeys() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID requestId = UUID.randomUUID();

        guard.tunnelDispatcherRequest("fp123", createTunnelPayload(requestId, timestamp));

        testClock.plus(TTL).plus(MAX_FUTURE_DRIFT).plusSeconds(1).plus(ReplyAttackGuard.PURGE_SAFETY_MARGIN);
        guard.purge();

        assertDoesNotThrow(() -> guard.tunnelDispatcherRequest("fp123", createTunnelPayload(requestId, testClock.millis())));
    }

    @Test
    void shouldDetectSessionRequestReplayForSameKey() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID requestId = UUID.randomUUID();
        byte[] dhKey = "same_dh_key".getBytes();

        var payload = new HandshakeSessionDTO.SessionRequestPayload(requestId, "salt".getBytes(), dhKey, timestamp);

        guard.sessionRequest(payload);

        assertThrows(
                SecurityException.class,
                () -> guard.sessionRequest(payload)
        );
    }

    @Test
    void shouldAllowSessionRequestsWithDifferentRequestIds() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        byte[] dhKey = "same_dh_key".getBytes();

        var payload1 = new HandshakeSessionDTO.SessionRequestPayload(UUID.randomUUID(), "salt1".getBytes(), dhKey, timestamp);
        var payload2 = new HandshakeSessionDTO.SessionRequestPayload(UUID.randomUUID(), "salt2".getBytes(), dhKey, timestamp);

        assertDoesNotThrow(() -> guard.sessionRequest(payload1));
        assertDoesNotThrow(() -> guard.sessionRequest(payload2));
    }

    @Test
    void shouldAllowSessionRequestsWithSameRequestIdButDifferentKeys() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID sameRequestId = UUID.randomUUID();

        var payload1 = new HandshakeSessionDTO.SessionRequestPayload(sameRequestId, "salt1".getBytes(), "dh1".getBytes(), timestamp);
        var payload2 = new HandshakeSessionDTO.SessionRequestPayload(sameRequestId, "salt2".getBytes(), "dh2".getBytes(), timestamp);

        assertDoesNotThrow(() -> guard.sessionRequest(payload1));
        assertDoesNotThrow(() -> guard.sessionRequest(payload2));
    }

    @Test
    void shouldDetectHandshakeRequestReplayForSameKey() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID requestId = UUID.randomUUID();
        byte[] rsa = "rsa_key".getBytes();
        byte[] dh = "dh_key".getBytes();

        var payload = new HandshakeRequestDTO.Payload(requestId, "salt".getBytes(), rsa, dh, timestamp);

        guard.handshakeRequest(payload);

        assertThrows(
                SecurityException.class,
                () -> guard.handshakeRequest(payload)
        );
    }

    @Test
    void shouldAllowHandshakeRequestIfRequestIdDiffers() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        byte[] rsa = "rsa_key".getBytes();
        byte[] dh = "dh_key".getBytes();

        var payload1 = new HandshakeRequestDTO.Payload(UUID.randomUUID(), "salt1".getBytes(), rsa, dh, timestamp);
        var payload2 = new HandshakeRequestDTO.Payload(UUID.randomUUID(), "salt2".getBytes(), rsa, dh, timestamp);

        assertDoesNotThrow(() -> guard.handshakeRequest(payload1));
        assertDoesNotThrow(() -> guard.handshakeRequest(payload2));
    }

    @Test
    void shouldAllowHandshakeRequestWithSameRequestIdButDifferentKeys() {
        advanceClockPastBootstrap();
        long timestamp = testClock.millis();
        UUID sameRequestId = UUID.randomUUID();

        var payload1 = new HandshakeRequestDTO.Payload(sameRequestId, "salt1".getBytes(), "rsa1".getBytes(), "dh1".getBytes(), timestamp);
        var payload2 = new HandshakeRequestDTO.Payload(sameRequestId, "salt2".getBytes(), "rsa2".getBytes(), "dh2".getBytes(), timestamp);

        assertDoesNotThrow(() -> guard.handshakeRequest(payload1));
        assertDoesNotThrow(() -> guard.handshakeRequest(payload2));
    }

    private void advanceClockPastBootstrap() {
        testClock.plus(MAX_FUTURE_DRIFT).plusSeconds(1);
    }

    private TunnelRequestDTO.Payload createTunnelPayload(UUID requestId, long timestamp) {
        return new TunnelRequestDTO.Payload(
                requestId,
                "command",
                new byte[0],
                new TunnelRequestDTO.Configuration(false),
                timestamp
        );
    }

    private static class TestClock extends Clock {
        private Instant currentInstant;

        TestClock(Instant start) {
            this.currentInstant = start;
        }

        TestClock plus(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
            return this;
        }

        TestClock minusMillis(long millis) {
            this.currentInstant = this.currentInstant.minusMillis(millis);
            return this;
        }

        TestClock plusSeconds(long seconds) {
            this.currentInstant = this.currentInstant.plusSeconds(seconds);
            return this;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}