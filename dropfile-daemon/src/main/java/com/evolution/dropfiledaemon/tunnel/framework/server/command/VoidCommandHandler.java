package com.evolution.dropfiledaemon.tunnel.framework.server.command;

public interface VoidCommandHandler extends CommandHandler<Void, Void> {

    @Override
    default Class<Void> getPayloadType() {
        return Void.class;
    }

    @Override
    default Void handle(Void unused) throws Exception {
        handle();
        return null;
    }

    String getCommandName();

    void handle() throws Exception;
}
