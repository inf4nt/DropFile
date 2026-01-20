package com.evolution.dropfiledaemon.util.function;

@FunctionalInterface
public interface IOConsumer<T> {

    void accept(T t) throws Exception;
}
