package com.evolution.dropfiledaemon.compress;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class ZstdCompressTunnelService
        implements CompressTunnelService {

    private final DaemonApplicationProperties daemonApplicationProperties;

    @Override
    public OutputStream compressWrapper(OutputStream outputStream) throws IOException {
        if (!daemonApplicationProperties.tunnelCompressEnabled) {
            return outputStream;
        }
        return new ZstdOutputStream(outputStream, daemonApplicationProperties.tunnelCompressLevel);
    }

    @Override
    public InputStream decompress(InputStream inputStream) throws IOException {
        if (!daemonApplicationProperties.tunnelCompressEnabled) {
            return inputStream;
        }
        return new ZstdInputStream(inputStream);
    }
}
