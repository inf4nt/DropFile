package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.Attributes;
import com.evolution.dropfile.common.LockableOperation;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class TunnelClientRefreshableSessionDecorator implements TunnelClient {

    // TODO create an env var
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final LockableOperation lockableOperationHandshakeTrustedOutStore;

    private final TunnelClient tunnelClient;

    private final ApiHandshakeFacade apiHandshakeFacade;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    @Override
    public InputStream stream(Request request) throws IOException {
        String fingerprint = request.getFingerprint();

        if (isSessionExpired(fingerprint)) {
            lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(fingerprint, () -> {
                if (isSessionExpired(fingerprint)) {
                    log.info("Session fingerprint {} has expired locally. Refreshing before request", fingerprint);
                    apiHandshakeFacade.systemHandshakeReconnect(fingerprint);
                }
            });
        }

        try {
            return tunnelClient.stream(request);
        } catch (Throwable e) {
            Attributes attributes = request.getAttributes();

            String failuresKey = getAttributeFailuresKey(fingerprint);
            String firstFailureTimeKey = getAttributeFirstFailTime(fingerprint);

            Instant firstFailureTime = attributes.computeIfAbsent(
                    firstFailureTimeKey,
                    _ -> Instant.now()
            );

            AtomicInteger failuresCounter = attributes.computeIfAbsent(
                    failuresKey,
                    _ -> new AtomicInteger()
            );

            int failuresCount = failuresCounter.incrementAndGet();

            if (failuresCount % 2 != 0) {
                throw e;
            }

            lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(fingerprint, () -> {
                if (isSessionExpired(fingerprint, firstFailureTime)) {
                    log.info("Force session refreshing for fingerprint {} after {} failed tunnel attempts",
                            fingerprint, failuresCount);
                    apiHandshakeFacade.systemHandshakeReconnect(fingerprint);
                } else {
                    log.info("Session for fingerprint {} was already updated after first failure time ({})",
                            fingerprint, firstFailureTime);
                }
            });
        }

        return tunnelClient.stream(request);
    }

    private boolean isSessionExpired(String fingerprint) {
        return isSessionExpired(fingerprint, null);
    }

    private boolean isSessionExpired(String fingerprint, @Nullable Instant firstFailureTime) {
        HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore
                .getRequired(fingerprint).getValue();

        Instant sessionLastUpdated = Stream.of(trustedOut.sessionUpdatedBySystem(), trustedOut.sessionUpdatedByUser())
                .max(Instant::compareTo)
                .orElseThrow();

        if (firstFailureTime != null && !sessionLastUpdated.isAfter(firstFailureTime)) {
            return true;
        }

        if (Instant.now().isAfter(sessionLastUpdated.plus(SESSION_TTL))) {
            return true;
        }

        boolean bothHaveSameHandshakeId = handshakeSessionOutStore
                .get(fingerprint)
                .stream()
                .anyMatch(it -> trustedOut.handshakeId().equals(it.getValue().handshakeId()));

        return !bothHaveSameHandshakeId;
    }

    private String getAttributeFailuresKey(String fingerprint) {
        return "FAIL:" + fingerprint;
    }

    private String getAttributeFirstFailTime(String fingerprint) {
        return "FIRST_FAIL_TIME:" + fingerprint;
    }
}
