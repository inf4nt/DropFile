package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

public interface HandshakeSessionOutStore extends KeyValueStore<HandshakeSessionOutStore.SessionOut> {

    record SessionOut(byte[] publicDH,
                      byte[] privateDH,
                      byte[] remotePublicDH,
                      Instant created) {

    }

    default Map.Entry<String, HandshakeSessionOutStore.SessionOut> getRequiredLatestCreated() {
        return getAll().entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().created()))
                .orElseThrow(() -> new RuntimeException("No sessions found"));
    }
}
