package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import org.springframework.stereotype.Component;

@Component
public class ApiConnectionsFacade {

    private final HandshakeStore handshakeStore;

    public ApiConnectionsFacade(HandshakeStore handshakeStore) {
        this.handshakeStore = handshakeStore;
    }

    public boolean revoke(String fingerprint) {
        return handshakeStore.trustedInStore().remove(fingerprint) != null;
    }

    public boolean disconnect(String fingerprint) {
        return handshakeStore.trustedOutStore().remove(fingerprint) != null;
    }
}
