package com.evolution.dropfiledaemon.handshake.store;

import java.util.List;
import java.util.Optional;

public interface HandshakeStoreManager {

    HandshakeRequestValue putRequest(String fingerprint, byte[] publicKey);

    HandshakeTrustValue putTrust(String fingerprint, byte[] publicKey, byte[] secret);

    Optional<HandshakeRequestValue> getRequest(String fingerprint);

    Optional<HandshakeTrustValue> getTrust(String fingerprint);

    List<HandshakeRequestValue> getRequests();

    List<HandshakeTrustValue> getTrusts();

    record HandshakeRequestValue(String fingerprint, byte[] publicKey) {
    }

    record HandshakeTrustValue(String fingerprint, byte[] publicKey, byte[] secret) {

    }
}
