package com.evolution.dropfiledaemon.handshake.store.runtime;

import com.evolution.dropfile.store.framework.RuntimeKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;

public class RuntimeHandshakeTrustedInStore
        extends RuntimeKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {
}
