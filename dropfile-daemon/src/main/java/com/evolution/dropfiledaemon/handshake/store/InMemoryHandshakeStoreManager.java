package com.evolution.dropfiledaemon.handshake.store;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryHandshakeStoreManager implements HandshakeStoreManager {

    private final Map<String, ValueContainer> store = new ConcurrentHashMap<>();

    @Override
    public HandshakeRequestValue request(String fingerprint, byte[] publicKey) {
        ValueContainer valueContainer = store
                .putIfAbsent(
                        fingerprint,
                        new ValueContainer(new HandshakeRequestValue(fingerprint, publicKey), null)
                );
        if (valueContainer == null) {
            return null;
        }
        return valueContainer.request();
    }

    @Override
    public HandshakeTrustValue requestToTrust(String fingerprint, byte[] secret) {
        ValueContainer valueContainer = store.computeIfPresent(
                fingerprint,
                (fp, value) -> {
                    if (value.trust() != null) {
                        throw new AlreadyTrustException(fingerprint);
                    }
                    HandshakeRequestValue requestValue = value.request();
                    HandshakeTrustValue trustValue = new HandshakeTrustValue(fingerprint, requestValue.publicKey(), secret);
                    return new ValueContainer(null, trustValue);
                }
        );
        if (valueContainer == null) {
            throw new NoRequestException(fingerprint);
        }
        return valueContainer.trust();
    }

    @Override
    public HandshakeTrustValue trust(String fingerprint, byte[] publicKey, byte[] secret) {
        ValueContainer valueContainer = store.computeIfAbsent(
                fingerprint,
                fp -> {
                    HandshakeTrustValue trustValue = new HandshakeTrustValue(fingerprint, publicKey, secret);
                    return new ValueContainer(null, trustValue);
                }
        );
        return valueContainer.trust();
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
