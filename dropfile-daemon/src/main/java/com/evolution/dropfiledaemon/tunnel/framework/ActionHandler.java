package com.evolution.dropfiledaemon.tunnel.framework;

public interface ActionHandler<T, R> {

    String getAction();

    Class<T> getPayloadType();

    R handle(T t);
}
