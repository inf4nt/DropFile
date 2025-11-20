package com.evolution.dropfile.common.function;

@FunctionalInterface
public interface Function<R, A, T extends Throwable> {

    R apply(A a) throws T;
}
