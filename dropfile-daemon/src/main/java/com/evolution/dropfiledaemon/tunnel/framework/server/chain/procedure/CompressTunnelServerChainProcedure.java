package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Component
public class CompressTunnelServerChainProcedure implements TunnelServerChainProcedure {

    private final CompressTunnelService compressTunnelService;

    @Override
    public void doChain(TunnelServerChainProcedureContext ctx,
                        TunnelServerChainProcedureProcessor processor) throws IOException {
        if (ctx.tunnelRequestPayload().configuration().compress()) {
            try (OutputStream compressOutputStream = compressTunnelService.compressWrapper(
                    ctx.outputStream())) {
                processor.doChain(ctx.withOutputStream(compressOutputStream));
            }
            return;
        }

        processor.doChain(ctx);
    }
}
