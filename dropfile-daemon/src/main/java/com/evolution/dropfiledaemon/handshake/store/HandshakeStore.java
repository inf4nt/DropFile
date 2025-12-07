package com.evolution.dropfiledaemon.handshake.store;

public record HandshakeStore(IncomingRequestKeyValueStore incomingRequestStore,
                             OutgoingRequestKeyValueStore outgoingRequestStore,
                             TrustedInKeyValueStore trustedInStore,
                             TrustedOutKeyValueStore trustedOutStore) {
}
