package com.evolution.dropfile.common;

import java.io.FilterInputStream;
import java.io.InputStream;

public class CloseShieldInputStream extends FilterInputStream {

    public CloseShieldInputStream(InputStream in) {
        super(in);
    }

    @Override
    public final void close() {
    }
}
