package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.DefaultTunnelServerDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TunnelServerDispatcherChainProcessorFactory {

    private final ObjectProvider<PayloadTimeoutValidationTunnelServerDispatcherChain> payloadTimeoutProvider;

    private final ObjectProvider<TrafficMonitorTunnelServerDispatcherChain> trafficMonitorProvider;

    private final ObjectProvider<HandshakeExpiredTunnelServerDispatcherChain> handshakeExpiredProvider;

    private final ObjectProvider<SessionExpiredTunnelServerDispatcherChain> sessionExpiredProvider;

    private final ObjectProvider<SuccessMarkerWriterTunnelServerDispatcherChain> successMarkerWriterProvider;

    private final ObjectProvider<CryptoTunnelServerDispatcherChain> cryptoTunnelProvider;

    private final ObjectProvider<CompressTunnelServerDispatcherChain> compressTunnelProvider;

    private final ObjectProvider<CommandTunnelServerDispatcherChain> commandTunnelProvider;

    public TunnelServerDispatcherChainProcessor createProcessor() {
        List<TunnelServerDispatcherChain> procedures = buildChain();
        return new DefaultTunnelServerDispatcherChainProcessor(procedures);
    }

    private List<TunnelServerDispatcherChain> buildChain() {
        return List.of(
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
}
