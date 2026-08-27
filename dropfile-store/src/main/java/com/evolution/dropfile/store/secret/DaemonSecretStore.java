package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.SingleValueStore;

public interface DaemonSecretStore
        extends SingleValueStore<DaemonSecret> {

    @Override
    default void validate(DaemonSecret value) {
        if (value == null) {
            throw new IllegalArgumentException("DaemonSecret is null");
        }
        String daemonToken = value.daemonToken();
        if (daemonToken == null || daemonToken.isBlank()) {
            throw new IllegalArgumentException("DaemonSecret daemonToken is empty");
        }
    }
}
