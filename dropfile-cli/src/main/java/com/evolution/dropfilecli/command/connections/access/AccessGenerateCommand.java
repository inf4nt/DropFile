package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "generate",
        aliases = {"-g", "--g"},
        description = "Generate access key command"
)
public class AccessGenerateCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionsAccessGenerate(false);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiConnectionsAccessInfoResponseDTO>() {
        };
    }
}
