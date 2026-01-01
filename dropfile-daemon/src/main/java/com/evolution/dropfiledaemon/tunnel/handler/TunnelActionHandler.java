package com.evolution.dropfiledaemon.tunnel.handler;

import com.evolution.dropfiledaemon.tunnel.TunnelRequestDTO;

public interface TunnelActionHandler {

    Object handle(TunnelRequestDTO.TunnelRequestPayload payload);
}
