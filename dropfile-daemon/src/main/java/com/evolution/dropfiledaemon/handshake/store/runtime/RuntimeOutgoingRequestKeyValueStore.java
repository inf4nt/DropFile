package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.OutgoingRequestKeyValueStore;

@Deprecated
public class RuntimeOutgoingRequestKeyValueStore
        extends RuntimeKeyValueStore<String, OutgoingRequestKeyValueStore.OutgoingRequestValue>
        implements OutgoingRequestKeyValueStore {
}
