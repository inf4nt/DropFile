package com.evolution.dropfiledaemon.tunnel.framework;

public interface TunnelDispatcher {

    TunnelResponseDTO dispatch(TunnelRequestDTO requestDTO);
}
