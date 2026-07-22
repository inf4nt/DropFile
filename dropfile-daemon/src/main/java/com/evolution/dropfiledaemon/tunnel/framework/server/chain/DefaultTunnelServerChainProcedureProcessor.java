package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.TunnelServerChainProcedure;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class DefaultTunnelServerChainProcedureProcessor implements TunnelServerChainProcedureProcessor {

    private int currentPosition = 0;

    private final List<TunnelServerChainProcedure> procedures;

    @Override
    public void doChain(TunnelServerChainProcedureContext ctx) throws IOException {
        if (currentPosition < procedures.size()) {
            TunnelServerChainProcedure procedure = procedures.get(currentPosition);
            currentPosition = currentPosition + 1;
            procedure.doChain(
                    ctx.withOutputStream(new CloseShieldOutputStream(ctx.outputStream())),
                    this
            );
        }
    }
}
