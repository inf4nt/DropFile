package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.AllowedOutKeyValueStore;

public class RuntimeAllowedOutKeyValueStore
        extends RuntimeKeyValueStore<String, AllowedOutKeyValueStore.AllowedOutValue>
        implements AllowedOutKeyValueStore {
}
