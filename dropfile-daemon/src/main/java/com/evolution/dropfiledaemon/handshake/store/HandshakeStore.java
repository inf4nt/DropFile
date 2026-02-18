package com.evolution.dropfiledaemon.handshake.store;

public record HandshakeStore(HandshakeTrustedOutStore trustedOutStore,
                             HandshakeTrustedInStore trustedInStore,
                             HandshakeSessionStore sessionStore) {

    // TODO Add SessionIn and SessionOut to separate it
    // -c status gets data from session and crashes if there are no connections output
    // HTTP response body: java.lang.RuntimeException. Message: Store
    // com.evolution.dropfiledaemon.handshake.store.crypto.CryptoHandshakeTrustedOutStore.
    // No key 67bca90384b45b12e3276fc7d807bdfe88a60f0a3abfad747e7d2fb91e5eef71 found
}
