package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.store.framework.bootstrap.BootstrapStoreInitializationProcedure;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class InstallationSeedBootstrapStoreInitializationProcedure
        implements BootstrapStoreInitializationProcedure {

    private final InstallationSeedBootstrapStore store;

    @Override
    public void init() {
        Optional<UUID> seed = store.get();
        if (seed.isEmpty()) {
            log.info("No seed found. Initialization in progress");
            store.save(UUID.randomUUID());
        }
    }
}
