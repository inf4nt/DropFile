package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;

public class RuntimeHandshakeTrustedOutStore
        extends RuntimeKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {
}
