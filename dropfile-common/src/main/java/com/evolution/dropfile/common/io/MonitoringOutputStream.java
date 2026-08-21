package com.evolution.dropfile.common.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MonitoringOutputStream extends FilterOutputStream {

    private final ThroughputMeter throughputMeter;

    public MonitoringOutputStream(OutputStream out, ThroughputMeter throughputMeter) {
        super(out);
        this.throughputMeter = throughputMeter;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        throughputMeter.add(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        if (len > 0) {
            throughputMeter.add(len);
        }
    }
}
