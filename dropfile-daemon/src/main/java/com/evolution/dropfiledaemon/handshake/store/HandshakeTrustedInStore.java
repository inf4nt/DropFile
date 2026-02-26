package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.time.Instant;

public interface HandshakeTrustedInStore extends KeyValueStore<HandshakeTrustedInStore.TrustedIn> {

    record TrustedIn(byte[] publicRSA,
                     byte[] privateRSA,
                     byte[] remoteRSA,
                     Instant created) {

    }
}
