package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedInKeyValueStore;

public class RuntimeTrustedInKeyValueStore
        extends RuntimeKeyValueStore<String, TrustedInKeyValueStore.TrustedInValue>
        implements TrustedInKeyValueStore {
}
