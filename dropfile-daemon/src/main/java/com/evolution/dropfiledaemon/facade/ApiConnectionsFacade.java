package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final ApplicationConfigStore applicationConfigStore;

    public void revoke(String fingerprint) {
        String key = applicationConfigStore.getHandshakeStore().trustedInStore().getRequiredByKeyStartWith(fingerprint)
                .getKey();
        applicationConfigStore.getHandshakeStore().trustedInStore().remove(key);
    }

    public void revokeAll() {
        applicationConfigStore.getHandshakeStore().trustedInStore().removeAll();
    }

    public void disconnect(String fingerprint) {
        String key = applicationConfigStore.getHandshakeStore().trustedOutStore().getRequiredByKeyStartWith(fingerprint).getKey();
        applicationConfigStore.getHandshakeStore().trustedOutStore().remove(key);
    }

    public void disconnectCurrent() {
        String key = applicationConfigStore.getHandshakeStore().sessionStore().getRequiredLatestUpdated().getKey();
        applicationConfigStore.getHandshakeStore().trustedOutStore().remove(key);
    }

    public void disconnectAll() {
        applicationConfigStore.getHandshakeStore().trustedOutStore().removeAll();
    }
}
