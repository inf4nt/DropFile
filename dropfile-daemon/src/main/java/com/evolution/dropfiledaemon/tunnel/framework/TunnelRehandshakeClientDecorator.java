package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.client.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.client.exception.TunnelClientException;
import com.evolution.dropfiledaemon.tunnel.framework.client.exception.TunnelClientSessionExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class TunnelRehandshakeClientDecorator implements TunnelClient, Purgeable {

    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    private final TunnelClient tunnelClient;

    private final ApiHandshakeFacade apiHandshakeFacade;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    @Override
    public InputStream stream(Request request) throws TunnelClientException {
        try {
            return tunnelClient.stream(request);
        } catch (Exception e) {
            TunnelClientSessionExpiredException expiredException = CommonUtils.getThrowable(e, TunnelClientSessionExpiredException.class);
            if (expiredException != null) {
                String fingerprint = request.getFingerprint();
                Object mutex = locks.computeIfAbsent(fingerprint, s -> new Object());
                synchronized (mutex) {
                    if (isSessionStillExpired(fingerprint, expiredException.getTimestamp())) {
                        log.info("Session fingerprint {} has been expired. Refreshing", fingerprint);
                        apiHandshakeFacade.systemHandshakeSessionRefresh(fingerprint, expiredException.getTimestamp());
                    }
                }
                return tunnelClient.stream(request);
            }
            throw e;
        }
    }

    private boolean isSessionStillExpired(String fingerprint, long failedSessionTimestamp) {
        HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore
                .getRequired(fingerprint)
                .getValue();

        Instant lastUpdated = Stream.of(trustedOut.sessionUpdatedByUser(), trustedOut.sessionUpdatedBySystem())
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(Instant.EPOCH);

        return lastUpdated.toEpochMilli() <= failedSessionTimestamp;
    }

    @Override
    public void purge() {
        locks.keySet().removeIf(fingerprint -> handshakeTrustedOutStore.get(fingerprint).isEmpty());
    }
}
