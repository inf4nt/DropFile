package com.evolution.dropfiledaemon.tunnel.framework;

public interface CommandHandler<T, R> {

    String getCommandName();

    Class<T> getPayloadType();

    R handle(T t);
}
