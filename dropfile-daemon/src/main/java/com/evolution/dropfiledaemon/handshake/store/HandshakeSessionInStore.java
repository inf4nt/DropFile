package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.time.Instant;

public interface HandshakeSessionInStore extends KeyValueStore<HandshakeSessionInStore.SessionIn> {

    record SessionIn(byte[] publicDH,
                     byte[] privateDH,
                     byte[] remotePublicDH,
                     Instant created) {

    }
}
