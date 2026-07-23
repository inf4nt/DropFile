package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;

import javax.crypto.SecretKey;
import java.io.OutputStream;
import java.util.Objects;

public record TunnelDispatcherChainProcessorContext(String fingerprint,
                                                    TunnelRequestDTO.Payload payload,
                                                    SecretKey secretKey,
                                                    OutputStream outputStream) {

    public TunnelDispatcherChainProcessorContext withOutputStream(OutputStream outputStream) {
        return new TunnelDispatcherChainProcessorContext(
                fingerprint,
                payload,
                secretKey,
                Objects.requireNonNull(outputStream)
        );
    }
}
