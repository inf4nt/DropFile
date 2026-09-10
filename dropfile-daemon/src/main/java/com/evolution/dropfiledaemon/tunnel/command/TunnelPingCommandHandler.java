package com.evolution.dropfiledaemon.tunnel.command;

import com.evolution.dropfiledaemon.tunnel.framework.server.command.VoidCommandHandler;
import org.springframework.stereotype.Component;

@Component
public class TunnelPingCommandHandler implements VoidCommandHandler {

    public static final String COMMAND_NAME = "tunnel-ping";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public void handle() {
        // nothing to do
    }
}
