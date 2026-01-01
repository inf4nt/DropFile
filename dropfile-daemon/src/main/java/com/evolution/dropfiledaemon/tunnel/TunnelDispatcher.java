package com.evolution.dropfiledaemon.tunnel;

public interface TunnelDispatcher {

    TunnelResponseDTO dispatch(TunnelRequestDTO requestDTO);
}
