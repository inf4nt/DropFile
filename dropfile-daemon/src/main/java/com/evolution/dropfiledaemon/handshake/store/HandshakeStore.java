package com.evolution.dropfiledaemon.handshake.store;

public record HandshakeStore(TrustedInKeyValueStore trustedInStore,
                             TrustedOutKeyValueStore trustedOutStore) {
}
