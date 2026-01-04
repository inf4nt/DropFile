package com.evolution.dropfiledaemon.tunnel.framework.handler;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;

public interface GlobalActionHandler {

    Object handle(TunnelRequestDTO.TunnelRequestPayload payload);
}
