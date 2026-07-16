package com.evolution.dropfile.common;

import java.io.InputStream;

public class CloseShieldInputStream extends InputStreamDecorator {

    public CloseShieldInputStream(InputStream in) {
        super(in);
    }

    @Override
    public final void close() {
    }
}
