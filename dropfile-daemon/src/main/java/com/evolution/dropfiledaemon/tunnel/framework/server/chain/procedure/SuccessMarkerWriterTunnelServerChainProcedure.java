package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class SuccessMarkerWriterTunnelServerChainProcedure implements TunnelServerChainProcedure {

    @Override
    public void doChain(TunnelServerChainProcedureContext ctx,
                        TunnelServerChainProcedureProcessor processor) throws IOException {
        ctx.outputStream().write(0x01);
        ctx.outputStream().flush();

        processor.doChain(ctx);
    }
}
