package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.util.Objects;
import java.util.UUID;

public interface HandshakeSessionInStore extends KeyValueStore<HandshakeSessionInStore.SessionIn> {

    record SessionIn(byte[] clientKey,
                     byte[] serverKey,
                     UUID sessionId) {

        public SessionIn {
            Objects.requireNonNull(clientKey);
            Objects.requireNonNull(serverKey);
            Objects.requireNonNull(sessionId);
        }
    }
}
