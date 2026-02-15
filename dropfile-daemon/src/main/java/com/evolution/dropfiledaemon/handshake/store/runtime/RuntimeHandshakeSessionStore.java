package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.store.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;

public class RuntimeHandshakeSessionStore
        extends RuntimeKeyValueStore<HandshakeSessionStore.SessionValue>
        implements HandshakeSessionStore {
}
