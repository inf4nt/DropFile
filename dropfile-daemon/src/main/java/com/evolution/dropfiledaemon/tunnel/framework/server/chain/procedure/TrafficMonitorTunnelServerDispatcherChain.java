package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class TrafficMonitorTunnelServerDispatcherChain implements TunnelServerDispatcherChain {

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelServerDispatcherChainProcessor processor) throws IOException {
        try (OutputStream tunnelTrafficOutputStream = tunnelTrafficMonitor.outputStreamWrapper(
                ctx.fingerprint(),
                ctx.outputStream())) {
            processor.proceed(ctx.withOutputStream(tunnelTrafficOutputStream));
        }
    }
}
