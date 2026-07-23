package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;

import java.io.IOException;

public interface TunnelServerDispatcherChain {

    void doChain(TunnelDispatcherChainProcessorContext ctx,
                 TunnelServerDispatcherChainProcessor processor) throws IOException;
}
