package com.evolution.dropfiledaemon.compress;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class ZstdCompressTunnelService
        implements CompressTunnelService {

    private final ApplicationConfigStore applicationConfigStore;

    @Override
    public OutputStream compressWrapper(OutputStream outputStream) throws IOException {
        if (!active()) {
            return outputStream;
        }
        int compressLevel = getCompressLevel();
        return new ZstdOutputStream(outputStream, compressLevel);
    }

    @Override
    public InputStream decompress(InputStream inputStream) throws IOException {
        if (!active()) {
            return inputStream;
        }
        return new ZstdInputStream(inputStream);
    }

    private int getCompressLevel() {
        return applicationConfigStore.getDaemonAppConfigStore().getRequired().compressTunnelLevel();
    }

    private boolean active() {
        return applicationConfigStore.getDaemonAppConfigStore().getRequired().compressTunnelActive();
    }
}
