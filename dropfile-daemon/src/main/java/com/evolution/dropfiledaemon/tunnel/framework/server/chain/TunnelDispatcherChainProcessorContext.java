package com.evolution.dropfiledaemon.tunnel.framework.server.chain;

import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import lombok.With;

import javax.crypto.SecretKey;
import java.io.OutputStream;

@With
public record TunnelDispatcherChainProcessorContext(String fingerprint,
                                                    TunnelRequestDTO.TunnelRequestPayload tunnelRequestPayload,
                                                    SecretKey secretKey,
                                                    OutputStream outputStream) {
}
