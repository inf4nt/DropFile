package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelServerDispatcherStatus;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class SuccessMarkerWriterTunnelServerDispatcherChain implements TunnelServerDispatcherChain {

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelServerDispatcherChainProcessor processor) throws IOException {
        try (OutputStream outputStream = ctx.outputStream()) {
            outputStream.write(TunnelServerDispatcherStatus.OK.getStatusCode());
            outputStream.flush();
        }
        processor.proceed(ctx);
    }
}
