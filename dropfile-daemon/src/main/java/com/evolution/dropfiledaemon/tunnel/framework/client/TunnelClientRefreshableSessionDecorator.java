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
        Attributes attributes = request.getAttributes();

        String failuresKey = getAttributeFailuresKey(fingerprint);
        AtomicInteger failuresCounter = attributes.computeIfAbsent(failuresKey, _ -> new AtomicInteger(0));
        int failures = failuresCounter.get();

        boolean shouldRefresh = isSessionExpired(fingerprint)
                || failures >= 2;

        if (shouldRefresh) {
            lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(fingerprint, () -> {
                if (isSessionExpired(fingerprint)) {
                    log.info("Refreshing session for fingerprint {} (failures={}, proactiveOrAfterErrors=true)",
                            fingerprint, failures);
                    apiHandshakeFacade.systemHandshakeReconnect(fingerprint);
                } else {
                    log.debug("Session for fingerprint {} was already refreshed by another thread (failures={})",
                            fingerprint, failures);
                }
            });
        }

        try {
            InputStream result = tunnelClient.stream(request);
            failuresCounter.set(0);
            return result;
        } catch (Throwable e) {
            failuresCounter.incrementAndGet();
            throw e;
        }
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
}
