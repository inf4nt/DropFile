package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfile.common.InputStreamDecorator;
import com.evolution.dropfiledaemon.util.DownloadSpeedMeter;

import java.io.IOException;
import java.io.InputStream;

public class MonitoringInputStream extends InputStreamDecorator {

    private final DownloadSpeedMeter downloadSpeedMeter;

    public MonitoringInputStream(InputStream in, DownloadSpeedMeter downloadSpeedMeter) {
        super(in);
        this.downloadSpeedMeter = downloadSpeedMeter;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result == -1) {
            return -1;
        }
        downloadSpeedMeter.addChunk(1);
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = super.read(b);
        if (result == -1) {
            return -1;
        }
        downloadSpeedMeter.addChunk(result);
        return result;
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
