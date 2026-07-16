package com.evolution.dropfile.common;

import java.io.OutputStream;

public class CloseShieldOutputStream extends OutputStreamDecorator {

    public CloseShieldOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public final void close() {

    }
}
