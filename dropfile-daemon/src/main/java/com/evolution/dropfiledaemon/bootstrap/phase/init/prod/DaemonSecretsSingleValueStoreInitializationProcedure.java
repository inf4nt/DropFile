package com.evolution.dropfiledaemon.bootstrap.phase.init.prod;

import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class DaemonSecretsSingleValueStoreInitializationProcedure
        implements SingleValueStoreInitializationProcedure {

    private final DaemonSecretsStore store;

    @Override
    public void init() {
        DaemonSecrets value = new DaemonSecrets(UUID.randomUUID().toString());
        store.save(value);
    }
}
