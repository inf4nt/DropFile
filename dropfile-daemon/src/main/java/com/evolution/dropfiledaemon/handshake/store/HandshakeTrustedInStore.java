package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.With;

import java.time.Duration;
import java.time.Instant;

public interface HandshakeTrustedInStore extends KeyValueStore<HandshakeTrustedInStore.TrustedIn> {

    @With
    record TrustedIn(HandshakeKeys handshake,
                     SessionKeys session,
                     Duration handshakeTtl,
                     Duration sessionTtl,
                     Instant created,
                     long sessionRefreshRequestTimestamp,
                     Instant sessionUpdatedByUser,
                     Instant sessionUpdatedBySystem,
                     Instant updatedByUser,
                     Instant updatedBySystem) {

    }

    record HandshakeKeys(byte[] publicRSA,
                         byte[] privateRSA,
                         byte[] remoteRSA) {
    }

    record SessionKeys(byte[] publicDH,
                       byte[] privateDH,
                       byte[] remotePublicDH) {
    }
}
