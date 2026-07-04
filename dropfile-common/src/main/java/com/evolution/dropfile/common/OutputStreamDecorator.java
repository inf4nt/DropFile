package com.evolution.dropfile.common;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
public class OutputStreamDecorator extends OutputStream {

    protected final OutputStream out;

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}