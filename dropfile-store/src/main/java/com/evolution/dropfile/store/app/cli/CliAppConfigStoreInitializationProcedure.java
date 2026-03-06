package com.evolution.dropfile.store.app.cli;

import com.evolution.dropfile.store.framework.single.StoreInitializationProcedure;

public class CliAppConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<CliAppConfigStore> {

    @Override
    public void init(CliAppConfigStore store) {
        store.init();

        CliAppConfig cliAppConfig = store.get().orElse(null);
        if (cliAppConfig != null) {
            return;
        }
        store.save(
                new CliAppConfig(
                        "127.0.0.1",
                        18181
                )
        );
    }
}
