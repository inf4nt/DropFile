package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "current",
        description = "Retrieve current connection"
)
public class CurrentConnectionCommand extends AbstractCommandHttpHandler<HandshakeApiTrustOutResponseDTO> {

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.getTrustLatest();
    }

    @Override
    protected TypeReference<HandshakeApiTrustOutResponseDTO> getTypeReference() {
        return new TypeReference<HandshakeApiTrustOutResponseDTO>() {
        };
    }
}
