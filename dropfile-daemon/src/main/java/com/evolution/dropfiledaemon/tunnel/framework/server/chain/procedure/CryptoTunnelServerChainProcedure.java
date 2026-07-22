package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
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
public class CryptoTunnelServerChainProcedure implements TunnelServerChainProcedure {

    private final CryptoTunnel cryptoTunnel;

    @Override
    public void doChain(TunnelServerChainProcedureContext ctx,
                        TunnelServerChainProcedureProcessor processor) throws IOException {
        try (OutputStream encryptOutputStream = cryptoTunnel.encryptWrapper(
                ctx.outputStream(),
                ctx.secretKey())) {
            processor.doChain(ctx.withOutputStream(encryptOutputStream));
        }
    }
}
