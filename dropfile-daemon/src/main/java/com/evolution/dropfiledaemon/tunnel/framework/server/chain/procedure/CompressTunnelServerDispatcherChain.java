package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Component
public class CompressTunnelServerDispatcherChain implements TunnelServerDispatcherChain {

    private final CompressTunnelService compressTunnelService;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelServerDispatcherChainProcessor processor) throws IOException {
        if (ctx.payload().configuration().compress()) {
            try (OutputStream compressOutputStream = compressTunnelService.compressWrapper(
                    ctx.outputStream())) {
                processor.proceed(ctx.withOutputStream(compressOutputStream));
            }
            return;
        }

        processor.proceed(ctx);
    }
}
