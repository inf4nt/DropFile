package com.evolution.dropfile.store.app.daemon;

import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileDaemonAppConfigStore
        extends DefaultSingleValueStore<DaemonAppConfig>
        implements DaemonAppConfigStore {

    public JsonFileDaemonAppConfigStore(ObjectMapper objectMapper) {
        super(
                "daemonAppConfig",
                new SynchronizedFileKeyValueStore<>(
                        new FileProviderImpl("daemon.app.config.json"),
                        new JsonFileOperations<>(
                                objectMapper,
                                DaemonAppConfig.class
                        )
                )
        );
    }
}
