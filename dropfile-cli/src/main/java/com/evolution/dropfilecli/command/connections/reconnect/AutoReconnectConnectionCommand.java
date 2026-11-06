package com.evolution.dropfilecli.command.connections.reconnect;

import com.evolution.dropfile.common.dto.ApiHandshakeReconnectAutoResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "auto",
        description = "Reconnect by fingerprint, then by alias if needed",
        customSynopsis = {
                "dropf connections reconnect auto"
        }
)
public class AutoReconnectConnectionCommand extends AbstractCommandHttpHandler<ApiHandshakeReconnectAutoResponseDTO> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshakeReconnectAuto();
    }

    @Override
    protected TypeReference<ApiHandshakeReconnectAutoResponseDTO> getTypeReference() {
        return new TypeReference<ApiHandshakeReconnectAutoResponseDTO>() {
        };
    }
}