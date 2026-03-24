package com.evolution.dropfiledaemon.tunnel.framework.monitor;

import com.evolution.dropfiledaemon.util.DownloadSpeedMeter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MonitoringOutputStream extends FilterOutputStream {

    private final DownloadSpeedMeter downloadSpeedMeter;

    public MonitoringOutputStream(OutputStream out, DownloadSpeedMeter downloadSpeedMeter) {
        super(out);
        this.downloadSpeedMeter = downloadSpeedMeter;
    }


    @Override
    public void write(int b) throws IOException {
        super.write(b);
        downloadSpeedMeter.addChunk(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        downloadSpeedMeter.addChunk(len);
    }
}
