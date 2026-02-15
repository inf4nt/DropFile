package com.evolution.dropfiledaemon.handshake.store;

public record HandshakeStore(HandshakeTrustedOutStore trustedOutStore,
                             HandshakeTrustedInStore trustedInStore,
                             HandshakeSessionStore sessionStore) {
}
