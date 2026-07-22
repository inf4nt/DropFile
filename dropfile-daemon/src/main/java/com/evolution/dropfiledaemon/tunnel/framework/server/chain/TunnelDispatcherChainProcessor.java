package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import java.io.IOException;

public interface TunnelDispatcherChainProcessor {

    void proceed(TunnelDispatcherChainProcessorContext ctx) throws IOException;
}
