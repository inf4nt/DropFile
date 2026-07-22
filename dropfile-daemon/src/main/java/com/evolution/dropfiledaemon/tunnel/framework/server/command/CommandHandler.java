package com.evolution.dropfiledaemon.tunnel.framework.server.command;

public interface CommandHandler<T, R> {

    String getCommandName();

    Class<T> getPayloadType();

    R handle(T t);
}
