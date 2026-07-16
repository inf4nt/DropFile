package com.evolution.dropfile.common;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CloseShieldOutputStream extends FilterOutputStream {

    public CloseShieldOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public final void close() throws IOException {
        out.flush();
    }
}
