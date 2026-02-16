package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ApiConnectionsFacade {

    private final ApplicationConfigStore applicationConfigStore;

    public void revoke(String fingerprint) {
        String key = applicationConfigStore.getHandshakeContextStore().trustedInStore().getRequiredByKeyStartWith(fingerprint)
                .getKey();
        applicationConfigStore.getHandshakeContextStore().trustedInStore().remove(key);
    }

    public void revokeAll() {
        applicationConfigStore.getHandshakeContextStore().trustedInStore().removeAll();
    }

    public void disconnect(String fingerprint) {
        String key = applicationConfigStore.getHandshakeContextStore().trustedOutStore().getRequiredByKeyStartWith(fingerprint).getKey();
        applicationConfigStore.getHandshakeContextStore().trustedOutStore().remove(key);
    }

    public void disconnectCurrent() {
        String key = applicationConfigStore.getHandshakeContextStore().sessionStore().getRequiredLatestUpdated().getKey();
        applicationConfigStore.getHandshakeContextStore().trustedOutStore().remove(key);
    }

    public void disconnectAll() {
        applicationConfigStore.getHandshakeContextStore().trustedOutStore().removeAll();
    }
}
