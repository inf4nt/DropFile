package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfiledaemon.util.DownloadSpeedMeter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MonitoringInputStream extends FilterInputStream {

    private final DownloadSpeedMeter downloadSpeedMeter;

    public MonitoringInputStream(InputStream in, DownloadSpeedMeter downloadSpeedMeter) {
        super(in);
        this.downloadSpeedMeter = downloadSpeedMeter;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b == -1) {
            return -1;
        }
        downloadSpeedMeter.addChunk(1);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result == -1) {
            return -1;
        }
        downloadSpeedMeter.addChunk(result);
        return result;
    }
}
