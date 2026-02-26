package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.SingleValueStore;

public interface DaemonSecretsStore
        extends SingleValueStore<DaemonSecrets> {

    @Override
    default void validate(DaemonSecrets value) {
        if (value == null) {
            throw new RuntimeException("DaemonSecretsEntry is null");
        }
        String daemonToken = value.daemonToken();
        if (daemonToken == null || daemonToken.trim().isEmpty()) {
            throw new RuntimeException("DaemonSecretsEntry daemonToken is empty");
        }
    }
}
