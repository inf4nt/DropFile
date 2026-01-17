package com.evolution.dropfiledaemon.tunnel.framework;

import java.io.OutputStream;

public interface TunnelDispatcher {

    TunnelResponseDTO dispatch(TunnelRequestDTO requestDTO);

    void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream);
}
