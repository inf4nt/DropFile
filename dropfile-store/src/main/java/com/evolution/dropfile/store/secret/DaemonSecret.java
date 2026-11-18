package com.evolution.dropfile.store.secret;

import lombok.With;

import java.util.Objects;
import java.util.UUID;

@With
public record DaemonSecret(UUID daemonToken,
                           byte[] publicKey,
                           byte[] privateKey) {

    public DaemonSecret {
        Objects.requireNonNull(daemonToken);
        Objects.requireNonNull(publicKey);
        Objects.requireNonNull(privateKey);
    }
}
