package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
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
public class CryptoTunnelServerDispatcherChain implements TunnelServerDispatcherChain {

    private final CryptoTunnel cryptoTunnel;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelServerDispatcherChainProcessor processor) throws IOException {
        try (OutputStream encryptOutputStream = cryptoTunnel.encryptWrapper(
                ctx.outputStream(),
                ctx.secretKey())) {
            processor.proceed(ctx.withOutputStream(encryptOutputStream));
        }
    }
}
