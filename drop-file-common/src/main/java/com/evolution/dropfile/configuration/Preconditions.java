package com.evolution.dropfile.configuration;

import java.util.function.Supplier;

public class Preconditions {

    public static void checkState(boolean condition, String message) {
        if (!condition) {
            if (message == null) {
                throw new IllegalStateException();
            }
            throw new IllegalStateException(message);
        }
    }

    public static void checkState(Supplier<Boolean> conditionSupplier, String message) {
        try {
            boolean condition = conditionSupplier.get();
            checkState(condition, message);
        } catch (Exception e) {
            throw new IllegalStateException(message, e);
        }
    }
}
