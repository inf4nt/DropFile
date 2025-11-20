package com.evolution.dropfile.common.function;

@FunctionalInterface
public interface Supplier<R, E extends Exception> {

    R get() throws E;
}
