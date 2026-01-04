package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.single.StoreInitializationProcedure;

import java.util.Optional;

public class AppConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<AppConfigStore> {
    @Override
    public void init(AppConfigStore store) {
        Optional<AppConfig> configOptional = store.get();
        if (configOptional.isPresent()) {
            return;
        }

        Integer daemonPort = 18181;
        AppConfig config = new AppConfig(
                new AppConfig.CliAppConfig(
                        "127.0.0.1",
                        daemonPort
                ),
                new AppConfig.DaemonAppConfig(
                        ".dropfile",
                        daemonPort
                )
        );
        store.save(config);
    }
}
