package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.AllowedInKeyValueStore;

public class RuntimeAllowedInKeyValueStore
        extends RuntimeKeyValueStore<String, AllowedInKeyValueStore.AllowedInValue>
        implements AllowedInKeyValueStore {
}
