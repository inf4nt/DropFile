package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

public interface HandshakeTrustedInStore extends KeyValueStore<HandshakeTrustedInStore.TrustedIn> {

    @With
    record TrustedIn(HandshakeKeys handshake,
                     Instant created,
                     Instant sessionUpdated,
                     Instant updated,
                     UUID handshakeId) {

    }

    record HandshakeKeys(byte[] publicRSA,
                         byte[] privateRSA,
                         byte[] remoteRSA) {
    }
}
