package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.ApiHandshakeStatusResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Retrieve status of current connection"
)
public class StatusConnectionCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshakeStatus();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiHandshakeStatusResponseDTO>() {
        };
    }
}
