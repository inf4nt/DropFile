package com.evolution.dropfiledaemon.handshake.store;

public record HandshakeStore(HandshakeTrustedOutStore trustedOutStore,
                             HandshakeTrustedInStore trustedInStore,
                             HandshakeSessionOutStore sessionOutStore,
                             HandshakeSessionInStore sessionInStore) {
}
