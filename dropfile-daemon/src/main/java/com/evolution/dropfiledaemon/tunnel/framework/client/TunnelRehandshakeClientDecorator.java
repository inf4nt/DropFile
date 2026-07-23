package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class TunnelRehandshakeClientDecorator implements TunnelClient, Purgeable {

    // TODO create an env var
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    private final TunnelClient tunnelClient;

    private final ApiHandshakeFacade apiHandshakeFacade;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    @Override
    public InputStream stream(Request request) {
        String fingerprint = request.getFingerprint();
        if (isSessionExpired(fingerprint)) {
            Object lock = locks.computeIfAbsent(fingerprint, __ -> new Object());
            synchronized (lock) {
                if (isSessionExpired(fingerprint)) {
                    log.info("Session fingerprint {} has been expired. Refreshing", fingerprint);
                    apiHandshakeFacade.systemHandshakeSessionRefresh(fingerprint);
                }
            }
        }
        return tunnelClient.stream(request);
    }

    private boolean isSessionExpired(String fingerprint) {
        HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore
                .getRequired(fingerprint).getValue();
        Instant sessionLastUpdated = Stream.of(trustedOut.sessionUpdatedBySystem(), trustedOut.sessionUpdatedByUser())
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElseThrow();

        return Instant.now().isAfter(sessionLastUpdated.plus(SESSION_TTL));
    }

    @Override
    public void purge() {
        locks.keySet().removeIf(fingerprint -> handshakeTrustedOutStore.get(fingerprint).isEmpty());
    }
}
