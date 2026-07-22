package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfiledaemon.tunnel.framework.server.chain.DefaultTunnelServerChainProcedureProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TunnelServerChainFactory {

    private final ObjectProvider<PayloadTimeoutValidationTunnelServerChainProcedure> payloadTimeoutProvider;

    private final ObjectProvider<TrafficMonitorTunnelServerChainProcedure> trafficMonitorProvider;

    private final ObjectProvider<HandshakeExpiredTunnelServerChainProcedure> handshakeExpiredProvider;

    private final ObjectProvider<SessionExpiredTunnelServerChainProcedure> sessionExpiredProvider;

    private final ObjectProvider<SuccessMarkerWriterTunnelServerChainProcedure> successMarkerWriterProvider;

    private final ObjectProvider<CryptoTunnelServerChainProcedure> cryptoTunnelProvider;

    private final ObjectProvider<CompressTunnelServerChainProcedure> compressTunnelProvider;

    private final ObjectProvider<CommandTunnelServerChainProcedure> commandTunnelProvider;

    public TunnelServerChainProcedureProcessor createProcessor() {
        List<TunnelServerChainProcedure> procedures = buildChain();
        return new DefaultTunnelServerChainProcedureProcessor(procedures);
    }

    private List<TunnelServerChainProcedure> buildChain() {
        return  List.of(
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
