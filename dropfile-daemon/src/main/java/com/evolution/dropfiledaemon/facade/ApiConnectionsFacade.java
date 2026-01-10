package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;
import org.springframework.stereotype.Component;

@Component
public class ApiConnectionsFacade {

    private final HandshakeStore handshakeStore;

    public ApiConnectionsFacade(HandshakeStore handshakeStore) {
        this.handshakeStore = handshakeStore;
    }

    public boolean revoke(String fingerprint) {
        TrustedInKeyValueStore.TrustedInValue remove = handshakeStore.trustedInStore().remove(fingerprint);
        return remove != null;
    }
}
