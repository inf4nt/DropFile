package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.LockableOperation;
import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfile.common.ThrowableUtils;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.exception.TunnelClientProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class TunnelClientRefreshableSessionDecorator implements TunnelClient, Purgeable {

    // TODO create an env var
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final Map<String, SuspectedSessionEnvelope> suspectSessions = new ConcurrentHashMap<>();

    private final LockableOperation lockableOperationHandshakeTrustedOutStore;

    private final TunnelClient tunnelClient;

    private final ApiHandshakeFacade apiHandshakeFacade;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final HandshakeSessionOutStore handshakeSessionOutStore;

    @Override
    public void purge() {
        suspectSessions.keySet().removeIf(fingerprint -> handshakeTrustedOutStore.get(fingerprint).isEmpty());
    }

    @Override
    public InputStream stream(Request request) throws IOException {
        String fingerprint = request.getFingerprint();

        if (isSessionHasToBeRefreshed(fingerprint)) {
            try {
                refreshSession(fingerprint);
            } catch (Exception refreshSessionException) {
                throw new IOException(
                        "Failed to process tunnel request for fingerprint %s command %s due to session refresh failure"
                                .formatted(fingerprint, request.getCommand()),
                        refreshSessionException
                );
            }
        }

        try {
            InputStream stream = tunnelClient.stream(request);
            clearResolvedSuspect(fingerprint);
            return stream;
        } catch (TunnelClientProtocolException tunnelException) {
            if (ThrowableUtils.checkThrowable(tunnelException, InterruptedException.class)) {
                throw tunnelException;
            }

            log.info("Tunnel request for fingerprint {} command {} failed, marking session as suspect and attempting refresh. Exception {}",
                    fingerprint, request.getCommand(), tunnelException.getMessage(), tunnelException);
            suspectSessions.put(fingerprint, new SuspectedSessionEnvelope(Instant.now()));

            try {
                refreshSession(fingerprint);
            } catch (Exception refreshSessionException) {
                tunnelException.addSuppressed(refreshSessionException);
                throw tunnelException;
            }
        }

        try {
            InputStream stream = tunnelClient.stream(request);
            clearResolvedSuspect(fingerprint);
            return stream;
        } catch (TunnelClientProtocolException tunnelException) {
            if (ThrowableUtils.checkThrowable(tunnelException, InterruptedException.class)) {
                throw tunnelException;
            }

            suspectSessions.put(fingerprint, new SuspectedSessionEnvelope(Instant.now()));
            throw tunnelException;
        }
    }

    private void refreshSession(String fingerprint) throws IOException {
        try {
            lockableOperationHandshakeTrustedOutStore.executeWithKeyLock(fingerprint, () -> {
                if (isSessionHasToBeRefreshed(fingerprint)) {
                    log.info("Refreshing session for fingerprint {} (proactive refresh)", fingerprint);
                    apiHandshakeFacade.systemHandshakeReconnect(fingerprint);
                } else {
                    log.debug("Session for fingerprint {} was already refreshed by another thread", fingerprint);
                }
                clearResolvedSuspect(fingerprint);
            });
        } catch (Exception e) {
            throw ThrowableUtils.rethrowIOException(e);
        }
    }

    private boolean isSessionHasToBeRefreshed(String fingerprint) {
        HandshakeTrustedOutStore.TrustedOut trustedOut = handshakeTrustedOutStore
                .getRequired(fingerprint).getValue();

        Instant sessionLastUpdated = trustedOut.updated();

        if (Instant.now().isAfter(sessionLastUpdated.plus(SESSION_TTL))) {
            return true;
        }

        boolean bothHaveSameHandshakeId = handshakeSessionOutStore
                .get(fingerprint)
                .stream()
                .anyMatch(it -> trustedOut.handshakeId().equals(it.getValue().handshakeId()));

        if (!bothHaveSameHandshakeId) {
            return true;
        }

        SuspectedSessionEnvelope suspectedSessionEnvelope = suspectSessions.get(fingerprint);

        if (suspectedSessionEnvelope == null) {
            return false;
        }

        if (trustedOut.updated().isAfter(suspectedSessionEnvelope.failAt())) {
            return false;
        }

        return true;
    }

    private void clearResolvedSuspect(String fingerprint) {
        CommonUtils.executeSafety(() -> {
            SuspectedSessionEnvelope suspected = suspectSessions.get(fingerprint);

            if (suspected == null) {
                return;
            }

            Instant sessionUpdated = handshakeTrustedOutStore
                    .getRequired(fingerprint)
                    .getValue()
                    .updated();

            if (sessionUpdated.isAfter(suspected.failAt())) {
                suspectSessions.remove(fingerprint, suspected);
            }
        });
    }

    private record SuspectedSessionEnvelope(Instant failAt) {
    }
}
