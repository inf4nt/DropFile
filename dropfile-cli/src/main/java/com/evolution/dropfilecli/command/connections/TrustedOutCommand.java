package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.HandshakeApiTrustOutResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TrustedOutCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public TrustedOutCommand(DaemonClient daemonClient,
                             ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getTrustOut();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<HandshakeApiTrustOutResponseDTO> values = objectMapper
                .readValue(
                        response.body(),
                        new TypeReference<List<HandshakeApiTrustOutResponseDTO>>() {
                        }
                );
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No trusted-out connections found");
        }
    }
}
