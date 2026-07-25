package com.evolution.dropfile.common;

import java.io.FilterInputStream;
import java.io.InputStream;

public final class CloseShieldInputStream extends FilterInputStream {

    private CloseShieldInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() {
    }

    public static CloseShieldInputStream stream(InputStream inputStream) {
        if (inputStream instanceof CloseShieldInputStream closeShieldInputStream) {
            return closeShieldInputStream;
        }
        return new CloseShieldInputStream(inputStream);
    }
}
