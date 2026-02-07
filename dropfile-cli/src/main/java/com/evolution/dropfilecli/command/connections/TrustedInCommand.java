package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.HandshakeApiTrustInResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "trusted-in",
        aliases = {"--in", "-in", "--i", "-i"},
        description = "Retrieve trusted-in connections"
)
public class TrustedInCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getTrustIn();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<HandshakeApiTrustInResponseDTO>>() {
        };
    }
}
