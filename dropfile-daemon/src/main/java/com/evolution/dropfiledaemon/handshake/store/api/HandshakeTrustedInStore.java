package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public interface HandshakeTrustedInStore extends KeyValueStore<HandshakeTrustedInStore.TrustedIn> {

    @With
    record TrustedIn(HandshakeKeys handshake,
                     UUID sessionId,
                     Instant created,
                     Instant updated) {

        public TrustedIn {
            Objects.requireNonNull(handshake);
            Objects.requireNonNull(sessionId);
            Objects.requireNonNull(created);
            Objects.requireNonNull(updated);
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
