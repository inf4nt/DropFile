package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "download",
        aliases = {"-d", "--d"},
        description = "Download file"
)
public class ConnectionsShareDownloadCommand extends AbstractCommandHttpHandler {

    @CommandLine.Option(names = {"-id", "--id"}, required = true)
    private String id;

    @CommandLine.Option(names = {"-filename", "--filename", "-f", "--f"})
    private String filename;

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionsShareDownload(id, filename);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiConnectionsShareDownloadResponseDTO>() {
        };
    }
}
