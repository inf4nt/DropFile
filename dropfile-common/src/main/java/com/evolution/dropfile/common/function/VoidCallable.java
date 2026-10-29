package com.evolution.dropfile.common.function;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface VoidCallable {

    void call() throws Exception;

    default Callable<Void> toCallable() {
        return () -> {
            call();
            return null;
        };
    }
}
