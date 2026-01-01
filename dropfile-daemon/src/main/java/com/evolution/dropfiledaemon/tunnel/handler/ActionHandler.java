package com.evolution.dropfiledaemon.tunnel.handler;

import com.evolution.dropfiledaemon.tunnel.TunnelRequestDTO;

public interface ActionHandler {
    String getAction();

    Object handle(TunnelRequestDTO.TunnelRequestPayload payload);
}
