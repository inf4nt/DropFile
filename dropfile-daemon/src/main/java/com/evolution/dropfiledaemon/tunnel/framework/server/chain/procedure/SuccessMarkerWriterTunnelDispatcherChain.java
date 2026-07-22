package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class SuccessMarkerWriterTunnelDispatcherChain implements TunnelDispatcherChain {

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelDispatcherChainProcessor processor) throws IOException {
        try (OutputStream outputStream = ctx.outputStream()) {
            outputStream.write(TunnelDispatcher.TunnelDispatcherStatus.OK.getStatusCode());
            outputStream.flush();
        }
        processor.proceed(ctx);
    }
}
