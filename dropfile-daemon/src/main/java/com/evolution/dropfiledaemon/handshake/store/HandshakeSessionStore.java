package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

public interface HandshakeSessionStore extends KeyValueStore<HandshakeSessionStore.SessionValue> {

    record SessionValue(byte[] publicDH,
                        byte[] privateDH,
                        byte[] remotePublicDH,
                        Instant updated) {

    }

    default Map.Entry<String, HandshakeSessionStore.SessionValue> getRequiredLatestUpdated() {
        return getAll().entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new RuntimeException("No sessions found"));
    }
}
