package com.evolution.dropfiledaemon.handshake.tunnel;

import com.evolution.dropfiledaemon.tunnel.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.handler.ActionHandler;
import org.springframework.stereotype.Component;

@Component
public class PingActionHandler implements ActionHandler {
    @Override
    public String getAction() {
        return "ping";
    }

    @Override
    public Object handle(TunnelRequestDTO.TunnelRequestPayload payload) {
        return "pong";
    }
}
