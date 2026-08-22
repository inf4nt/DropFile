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
        return new GZIPOutputStream(outputStream) {{
            def.setLevel(daemonApplicationProperties.daemonTunnelServerCompressLevel);
        }};
    }

    @Override
    public InputStream decompress(InputStream inputStream) throws IOException {
        return new GZIPInputStream(inputStream);
    }
}
