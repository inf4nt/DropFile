package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.TunnelServerDispatcherChain;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class DefaultTunnelServerDispatcherChainProcessor implements TunnelServerDispatcherChainProcessor {

    private int currentPosition = 0;

    private final List<TunnelServerDispatcherChain> procedures;

    @Override
    public void proceed(TunnelDispatcherChainProcessorContext ctx) throws IOException {
        if (currentPosition < procedures.size()) {
            TunnelServerDispatcherChain procedure = procedures.get(currentPosition);
            currentPosition = currentPosition + 1;
            procedure.doChain(
                    ctx.withOutputStream(new CloseShieldOutputStream(ctx.outputStream())),
                    this
            );
        }
    }
}
