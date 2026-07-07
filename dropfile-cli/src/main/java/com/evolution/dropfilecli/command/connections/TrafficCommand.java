package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.TunnelTrafficResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
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

public class TrafficCommand extends AbstractCommandHttpHandler<List<TunnelTrafficResponseDTO>> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsTraffic();
    }

    @Override
    protected TypeReference<List<TunnelTrafficResponseDTO>> getTypeReference() {
        return new TypeReference<List<TunnelTrafficResponseDTO>>() {
        };
    }
}
