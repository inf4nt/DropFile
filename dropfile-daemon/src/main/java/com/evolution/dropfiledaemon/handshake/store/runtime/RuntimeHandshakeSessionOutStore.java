package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;

public class RuntimeHandshakeSessionOutStore
        extends RuntimeKeyValueStore<HandshakeSessionStore.SessionValue>
        implements HandshakeSessionOutStore {
}
