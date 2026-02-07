package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "current",
        description = "Retrieve current connection"
)
public class CurrentConnectionCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.getTrustLatest();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<HandshakeApiTrustOutResponseDTO>() {
        };
    }
}
