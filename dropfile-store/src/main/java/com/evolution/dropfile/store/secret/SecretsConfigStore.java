package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.store.single.SingleValueStore;

public interface SecretsConfigStore
        extends SingleValueStore<SecretsConfig> {

    @Override
    default void validate(SecretsConfig value) {
        if (value == null) {
            throw new RuntimeException("SecretsConfig is null");
        }
        String daemonToken = value.daemonToken();
        if (daemonToken == null || daemonToken.trim().isEmpty()) {
            throw new RuntimeException("SecretsConfig daemonToken is empty");
        }
    }
}
