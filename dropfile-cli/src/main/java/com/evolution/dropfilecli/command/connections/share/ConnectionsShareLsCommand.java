package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls",
        description = "ls shared files"
)
public class ConnectionsShareLsCommand extends AbstractCommandHttpHandler {

    @CommandLine.Option(names = {"-id", "--id"}, split = ",", description = "List of ids")
    private List<String> ids;

    @Override
    public HttpResponse<byte[]> execute() {
        List<String> requestIds = ids == null ? Collections.emptyList() : ids;
        return daemonClient.connectionShareLs(requestIds);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<ApiConnectionsShareLsResponseDTO>>() {
        };
    }
}
