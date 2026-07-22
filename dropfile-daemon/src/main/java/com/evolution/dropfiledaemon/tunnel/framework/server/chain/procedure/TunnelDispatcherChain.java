package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;

import java.io.IOException;

public interface TunnelDispatcherChain {

    void doChain(TunnelDispatcherChainProcessorContext ctx,
                 TunnelDispatcherChainProcessor processor) throws IOException;
}
