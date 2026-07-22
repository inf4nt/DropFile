package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;

public interface TunnelDispatcher {

    void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException;

    enum TunnelDispatcherStatus {
        OK(1),
        SESSION_EXPIRED(2),
        HANDSHAKE_EXPIRED(3);

        @Getter
        private final int statusCode;

        TunnelDispatcherStatus(int statusCode) {
            this.statusCode = statusCode;
        }
    }
}
