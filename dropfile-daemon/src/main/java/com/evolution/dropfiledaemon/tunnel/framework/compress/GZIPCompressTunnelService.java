package com.evolution.dropfiledaemon.tunnel.framework.compress;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@RequiredArgsConstructor
@Component
public class GZIPCompressTunnelService implements CompressTunnelService {

    private final DaemonApplicationProperties daemonApplicationProperties;

    @Override
    public OutputStream compressWrapper(OutputStream outputStream) throws IOException {
        int level = daemonApplicationProperties.daemonTunnelServerCompressLevel;
        return new CustomLevelGZIPOutputStream(outputStream, level);
    }

    @Override
    public InputStream decompress(InputStream inputStream) throws IOException {
        return new GZIPInputStream(inputStream);
    }

    private static class CustomLevelGZIPOutputStream extends GZIPOutputStream {
        public CustomLevelGZIPOutputStream(OutputStream out, int level) throws IOException {
            super(out);
            this.def.setLevel(level);
        }
    }
}
