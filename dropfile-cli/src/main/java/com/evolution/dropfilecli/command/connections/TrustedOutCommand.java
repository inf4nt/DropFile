package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "trusted-out",
        aliases = {"--out", "-out", "--o", "-o"},
        description = "Retrieve trusted-out connections"
)
public class TrustedOutCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getTrustOut();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<HandshakeApiTrustOutResponseDTO>>() {
        };
    }
}
