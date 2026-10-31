package com.evolution.dropfile.common.function;

@FunctionalInterface
public interface InterruptibleVoidCallable {

    void call() throws InterruptedException;
}
