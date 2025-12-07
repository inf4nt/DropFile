package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;

public class RuntimeTrustedOutKeyValueStore
        extends RuntimeKeyValueStore<String, TrustedOutKeyValueStore.TrustedOutValue>
        implements TrustedOutKeyValueStore {
}
