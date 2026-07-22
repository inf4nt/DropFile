package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.TunnelDispatcherChain;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class DefaultTunnelDispatcherChainProcessor implements TunnelDispatcherChainProcessor {

    private int currentPosition = 0;

    private final List<TunnelDispatcherChain> procedures;

    @Override
    public void proceed(TunnelDispatcherChainProcessorContext ctx) throws IOException {
        if (currentPosition < procedures.size()) {
            TunnelDispatcherChain procedure = procedures.get(currentPosition);
            currentPosition = currentPosition + 1;
            procedure.doChain(
                    ctx.withOutputStream(new CloseShieldOutputStream(ctx.outputStream())),
                    this
            );
        }
    }
}
