package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionOutStore;

public class RuntimeHandshakeSessionOutStore
        extends RuntimeKeyValueStore<HandshakeSessionOutStore.SessionOut>
        implements HandshakeSessionOutStore {
}
