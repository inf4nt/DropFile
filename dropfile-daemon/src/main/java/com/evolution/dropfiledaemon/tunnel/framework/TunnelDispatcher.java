package com.evolution.dropfiledaemon.tunnel.framework;

import java.io.IOException;
import java.io.OutputStream;

public interface TunnelDispatcher {

    TunnelDispatcherContext dispatch(TunnelRequestDTO requestDTO);

    void transfer(TunnelDispatcherContext context, OutputStream outputStream) throws IOException;
}
