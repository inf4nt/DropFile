package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.util.UUID;

public interface HandshakeSessionOutStore extends KeyValueStore<HandshakeSessionOutStore.SessionOut> {

    record SessionOut(byte[] publicDH,
                      byte[] privateDH,
                      byte[] remotePublicDH,
                      byte[] sessionClientKey,
                      byte[] sessionServerKey,
                      UUID handshakeId) {
    }
}
