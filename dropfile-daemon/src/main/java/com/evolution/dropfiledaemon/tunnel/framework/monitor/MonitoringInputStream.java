package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfiledaemon.util.ThroughputMeter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MonitoringInputStream extends FilterInputStream {

    private final ThroughputMeter throughputMeter;

    public MonitoringInputStream(InputStream in, ThroughputMeter throughputMeter) {
        super(in);
        this.throughputMeter = throughputMeter;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result == -1) {
            return -1;
        }
        throughputMeter.add(1);
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result == -1) {
            return -1;
        }
        throughputMeter.add(result);
        return result;
    }
}
