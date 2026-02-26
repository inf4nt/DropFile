package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;

public class RuntimeHandshakeSessionInStore
        extends RuntimeKeyValueStore<HandshakeSessionStore.SessionValue>
        implements HandshakeSessionInStore {
}
