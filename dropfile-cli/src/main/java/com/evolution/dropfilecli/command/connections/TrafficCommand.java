package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "traffic",
        description = "Retrieve traffic"
)

public class TrafficCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsTraffic();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<TunnelTrafficResponseDTO>>() {
        };
    }
}
