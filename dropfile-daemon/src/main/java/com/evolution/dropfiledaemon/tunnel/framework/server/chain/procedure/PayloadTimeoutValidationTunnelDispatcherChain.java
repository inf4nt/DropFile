package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Component
public class PayloadTimeoutValidationTunnelDispatcherChain implements TunnelDispatcherChain {

    private final DaemonApplicationProperties daemonApplicationProperties;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelDispatcherChainProcessor processor) throws IOException {
        long requestTime = Math.abs(System.currentTimeMillis() - ctx.tunnelRequestPayload().timestamp());
        int tunnelServerPayloadLifeTime = daemonApplicationProperties.daemonTunnelServerPayloadLifeTime;
        if (requestTime > tunnelServerPayloadLifeTime) {
            throw new RuntimeException(
                    String.format(
                            "Tunnel request timeout exception. Expected %s actual %s",
                            tunnelServerPayloadLifeTime, requestTime
                    )
            );
        }

        processor.proceed(ctx);
    }
}
