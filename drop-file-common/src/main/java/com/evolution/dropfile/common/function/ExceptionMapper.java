package com.evolution.dropfile.common.function;

@FunctionalInterface
public interface ExceptionMapper<R, E extends Exception> {

    R map(E exception);
}
