package com.evolution.dropfiledaemon.tunnel.framework.exception;

import java.io.IOException;

public class TunnelClientProtocolException extends IOException {

    public TunnelClientProtocolException(String message) {
        super(message);
    }

    public TunnelClientProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
