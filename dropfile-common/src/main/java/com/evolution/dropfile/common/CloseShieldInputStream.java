package com.evolution.dropfile.common;

import java.io.FilterInputStream;
import java.io.InputStream;

// TODO make it as a final class and add a wrapper like CloseShieldOutputStream#stream
public class CloseShieldInputStream extends FilterInputStream {

    public CloseShieldInputStream(InputStream in) {
        super(in);
    }

    @Override
    public final void close() {
    }
}
