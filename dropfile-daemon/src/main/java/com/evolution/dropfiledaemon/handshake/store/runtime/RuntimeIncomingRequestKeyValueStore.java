package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.IncomingRequestKeyValueStore;

public class RuntimeIncomingRequestKeyValueStore
        extends RuntimeKeyValueStore<String, IncomingRequestKeyValueStore.IncomingRequestValue>
        implements IncomingRequestKeyValueStore {
}
