package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.SingleValueStore;

public interface DaemonSecretsStore
        extends SingleValueStore<DaemonSecrets> {

    @Override
    default void validate(DaemonSecrets value) {
        if (value == null) {
            throw new IllegalArgumentException("DaemonSecrets is null");
        }
        String daemonToken = value.daemonToken();
        if (daemonToken == null || daemonToken.isBlank()) {
            throw new IllegalArgumentException("DaemonSecrets daemonToken is empty");
        }
    }
}
