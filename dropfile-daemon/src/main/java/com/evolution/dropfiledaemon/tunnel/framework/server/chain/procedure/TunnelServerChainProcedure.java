package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureProcessor;

import java.io.IOException;

public interface TunnelServerChainProcedure {

    void doChain(TunnelServerChainProcedureContext ctx,
                 TunnelServerChainProcedureProcessor processor) throws IOException;
}
