package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls",
        description = "ls shared files"
)
public class ConnectionsShareLsCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionShareLs();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<ApiConnectionsShareLsResponseDTO>>() {
        };
    }
}
