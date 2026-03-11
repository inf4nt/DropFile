package com.evolution.dropfiledaemon.compress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface CompressTunnelService {

    OutputStream compressWrapper(OutputStream outputStream) throws IOException;

    InputStream decompress(InputStream inputStream) throws IOException;
}
