package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls",
        description = "Retrieve access keys"
)
public class AccessLsCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionsAccessLs();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<ApiConnectionsAccessInfoResponseDTO>>() {
        };
    }
}
