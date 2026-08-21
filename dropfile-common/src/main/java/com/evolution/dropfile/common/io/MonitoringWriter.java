package com.evolution.dropfile.common.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class MonitoringWriter extends FilterWriter {

    private final ThroughputMeter throughputMeter;

    public MonitoringWriter(Writer out, ThroughputMeter throughputMeter) {
        super(out);
        this.throughputMeter = throughputMeter;
    }

    @Override
    public void write(int c) throws IOException {
        out.write(c);
        throughputMeter.add(1);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
        if (len > 0) {
            throughputMeter.add(len);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        out.write(str, off, len);
        if (len > 0) {
            throughputMeter.add(len);
        }
    }
}