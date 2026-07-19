package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;

public class RuntimeHandshakeSessionOutStore
        extends RuntimeKeyValueStore<HandshakeSessionOutStore.SessionOut>
        implements HandshakeSessionOutStore {
}
