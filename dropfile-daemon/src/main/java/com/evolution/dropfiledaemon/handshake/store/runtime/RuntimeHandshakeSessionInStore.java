package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;

public class RuntimeHandshakeSessionInStore
        extends RuntimeKeyValueStore<HandshakeSessionInStore.SessionIn>
        implements HandshakeSessionInStore {
}
