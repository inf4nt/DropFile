package com.evolution.dropfiledaemon.tunnel.framework;

import java.io.IOException;
import java.io.OutputStream;

public interface TunnelDispatcher {

    void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException;
}
