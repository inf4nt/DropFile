package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public interface HandshakeTrustedInStore extends KeyValueStore<HandshakeTrustedInStore.TrustedIn> {

    @With
    record TrustedIn(HandshakeKeys handshake,
                     Instant created,
                     Instant updated,
                     UUID sessionId) {

        public TrustedIn {
            Objects.requireNonNull(handshake);
            Objects.requireNonNull(created);
            Objects.requireNonNull(updated);
            Objects.requireNonNull(sessionId);
        }
    }

    record HandshakeKeys(byte[] publicRSA,
                         byte[] privateRSA,
                         byte[] remoteRSA) {

        public HandshakeKeys {
            Objects.requireNonNull(publicRSA);
            Objects.requireNonNull(privateRSA);
            Objects.requireNonNull(remoteRSA);
        }
    }
}
