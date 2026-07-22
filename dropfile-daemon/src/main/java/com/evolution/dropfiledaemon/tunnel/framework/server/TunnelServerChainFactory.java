package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.DefaultTunnelDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TunnelServerChainFactory {

    private final ObjectProvider<PayloadTimeoutValidationTunnelDispatcherChain> payloadTimeoutProvider;

    private final ObjectProvider<TrafficMonitorTunnelDispatcherChain> trafficMonitorProvider;

    private final ObjectProvider<HandshakeExpiredTunnelDispatcherChain> handshakeExpiredProvider;

    private final ObjectProvider<SessionExpiredTunnelDispatcherChain> sessionExpiredProvider;

    private final ObjectProvider<SuccessMarkerWriterTunnelDispatcherChain> successMarkerWriterProvider;

    private final ObjectProvider<CryptoTunnelDispatcherChain> cryptoTunnelProvider;

    private final ObjectProvider<CompressTunnelDispatcherChain> compressTunnelProvider;

    private final ObjectProvider<CommandTunnelDispatcherChain> commandTunnelProvider;

    public TunnelDispatcherChainProcessor createProcessor() {
        List<TunnelDispatcherChain> procedures = buildChain();
        return new DefaultTunnelDispatcherChainProcessor(procedures);
    }

    private List<TunnelDispatcherChain> buildChain() {
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
