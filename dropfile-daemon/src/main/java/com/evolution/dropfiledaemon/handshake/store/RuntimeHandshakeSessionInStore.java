package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore;

public class RuntimeHandshakeSessionInStore
        extends RuntimeKeyValueStore<HandshakeSessionInStore.SessionIn>
        implements HandshakeSessionInStore {
}
