package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import com.evolution.dropfile.common.CloseShieldOutputStream;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class DefaultTunnelServerDispatcherChainProcessor implements TunnelServerDispatcherChainProcessor {

    private int currentPosition = 0;

    private final List<TunnelServerDispatcherChain> procedures;

    public DefaultTunnelServerDispatcherChainProcessor(ObjectProvider<PayloadTimeoutValidationTunnelServerDispatcherChain> payloadTimeoutProvider,
                                                       ObjectProvider<TrafficMonitorTunnelServerDispatcherChain> trafficMonitorProvider,
                                                       ObjectProvider<HandshakeExpiredTunnelServerDispatcherChain> handshakeExpiredProvider,
                                                       ObjectProvider<SessionExpiredTunnelServerDispatcherChain> sessionExpiredProvider,
                                                       ObjectProvider<SuccessMarkerWriterTunnelServerDispatcherChain> successMarkerWriterProvider,
                                                       ObjectProvider<CryptoTunnelServerDispatcherChain> cryptoTunnelProvider,
                                                       ObjectProvider<CompressTunnelServerDispatcherChain> compressTunnelProvider,
                                                       ObjectProvider<CommandTunnelServerDispatcherChain> commandTunnelProvider) {
        procedures = List.of(
                payloadTimeoutProvider.getObject(),
                trafficMonitorProvider.getObject(),
                handshakeExpiredProvider.getObject(),
                sessionExpiredProvider.getObject(),
                successMarkerWriterProvider.getObject(),
                cryptoTunnelProvider.getObject(),
                compressTunnelProvider.getObject(),
                commandTunnelProvider.getObject()
        );
    }

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
