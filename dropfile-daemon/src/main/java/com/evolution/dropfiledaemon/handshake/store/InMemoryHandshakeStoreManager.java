package com.evolution.dropfiledaemon.handshake.store;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryHandshakeStoreManager implements HandshakeStoreManager {

    private final Map<String, ValueContainer> store = new ConcurrentHashMap<>();

    @Override
    public HandshakeRequestValue putRequest(String fingerprint, byte[] publicKey) {
        store.put(
                fingerprint,
                new ValueContainer(new HandshakeRequestValue(fingerprint, publicKey), null)
        );
        return store.get(fingerprint).request();
    }

    @Override
    public HandshakeTrustValue putTrust(String fingerprint, byte[] publicKey, byte[] secret) {
        store.put(
                fingerprint,
                new ValueContainer(null, new HandshakeTrustValue(fingerprint, publicKey, secret))
        );
        return store.get(fingerprint).trust();
    }

    @Override
    public Optional<HandshakeRequestValue> getRequest(String fingerprint) {
        return Optional.ofNullable(store.get(fingerprint))
                .map(it -> it.request());
    }

    @Override
    public Optional<HandshakeTrustValue> getTrust(String fingerprint) {
        return Optional.ofNullable(store.get(fingerprint))
                .filter(it -> it != null)
                .map(it -> it.trust());
    }

    @Override
    public List<HandshakeRequestValue> getRequests() {
        return store.values().stream().map(it -> it.request())
                .filter(it -> it != null)
                .toList();
    }

    @Override
    public List<HandshakeTrustValue> getTrusts() {
        return store.values().stream().map(it -> it.trust())
                .filter(it -> it != null)
                .toList();
    }

    private record ValueContainer(HandshakeRequestValue request, HandshakeTrustValue trust) {
    }
}
