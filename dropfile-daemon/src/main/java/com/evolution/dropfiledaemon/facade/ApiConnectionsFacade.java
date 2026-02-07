package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import org.springframework.stereotype.Component;

@Component
public class ApiConnectionsFacade {

    private final HandshakeStore handshakeStore;

    public ApiConnectionsFacade(HandshakeStore handshakeStore) {
        this.handshakeStore = handshakeStore;
    }

    public void revoke(String fingerprint) {
        String key = handshakeStore.trustedInStore().getRequired(fingerprint)
                .getKey();
        handshakeStore.trustedInStore().remove(key);
    }

    public void revokeCurrent() {
        String key = handshakeStore.trustedInStore().getRequiredLatestUpdated().getKey();
        handshakeStore.trustedInStore().remove(key);
    }

    public void revokeAll() {
        handshakeStore.trustedInStore().removeAll();
    }

    public void disconnect(String fingerprint) {
        String key = handshakeStore.trustedOutStore().getRequired(fingerprint).getKey();
        handshakeStore.trustedOutStore().remove(key);
    }
}
