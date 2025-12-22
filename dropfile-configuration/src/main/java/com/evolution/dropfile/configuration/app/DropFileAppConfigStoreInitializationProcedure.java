package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.single.StoreInitializationProcedure;

import java.util.Optional;

public class DropFileAppConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<DropFileAppConfigStore> {
    @Override
    public void init(DropFileAppConfigStore store) {
        Optional<DropFileAppConfig> configOptional = store.get();
        if (configOptional.isPresent()) {
            return;
        }
        Integer daemonPort = 18181;
        DropFileAppConfig config = new DropFileAppConfig(
                new DropFileAppConfig.DropFileCliAppConfig(
                        "127.0.0.1",
                        daemonPort
                ),
                new DropFileAppConfig.DropFileDaemonAppConfig(
                        ".dropfile",
                        daemonPort,
                        null
                )
        );
        store.save(config);
    }
}
