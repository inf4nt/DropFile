package com.evolution.dropfiledaemon.tunnel.framework;

import lombok.Getter;

public enum TunnelServerDispatcherStatus {
    OK(1),
    SESSION_EXPIRED(2),
    HANDSHAKE_EXPIRED(3);

    @Getter
    private final int statusCode;

    TunnelServerDispatcherStatus(int statusCode) {
        this.statusCode = statusCode;
    }
}
