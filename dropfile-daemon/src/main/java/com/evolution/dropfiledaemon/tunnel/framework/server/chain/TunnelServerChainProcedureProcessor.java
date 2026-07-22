package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import java.io.IOException;

public interface TunnelServerChainProcedureProcessor {

    void doChain(TunnelServerChainProcedureContext ctx) throws IOException;
}
