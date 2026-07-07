package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.ApiHandshakeStatusResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Retrieve status of current connection"
)
public class StatusConnectionCommand extends AbstractCommandHttpHandler<ApiHandshakeStatusResponseDTO> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshakeStatus();
    }

    @Override
    protected TypeReference<ApiHandshakeStatusResponseDTO> getTypeReference() {
        return new TypeReference<ApiHandshakeStatusResponseDTO>() {
        };
    }
}
