package com.evolution.dropfiledaemon.handshake.store;

import java.util.List;
import java.util.Optional;

public interface HandshakeStoreManager {

    HandshakeRequestValue request(String fingerprint, byte[] publicKey);

    HandshakeTrustValue requestToTrust(String fingerprint, byte[] secret);

    HandshakeTrustValue trust(String fingerprint, byte[] publicKey, byte[] secret);

    Optional<HandshakeRequestValue> getRequest(String fingerprint);

    Optional<HandshakeTrustValue> getTrust(String fingerprint);

    List<HandshakeRequestValue> getRequests();

    List<HandshakeTrustValue> getTrusts();
    
    record HandshakeRequestValue(String fingerprint, byte[] publicKey) {
    }

    record HandshakeTrustValue(String fingerprint, byte[] publicKey, byte[] secret) {

    }

    class AlreadyTrustException extends RuntimeException {
        public AlreadyTrustException(String fingerprint) {
            super("Already trusted fingerprint " + fingerprint);
        }
    }

    class NoRequestException extends RuntimeException {
        public NoRequestException(String fingerprint) {
            super("No request for fingerprint " + fingerprint);
        }
    }
}
