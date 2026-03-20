package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "download",
        aliases = {"-d", "--d"},
        description = "Download file"
)
public class ConnectionsShareDownloadCommand extends AbstractCommandHttpHandler {

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1..*")
    private List<DownloadItem> items;

    @Getter
    public static class DownloadItem {
        @CommandLine.Option(names = {"-id", "--id"}, required = true)
        String id;

        @CommandLine.Option(names = {"-filename", "--filename", "-f", "--f"})
        String filename;
    }

    @CommandLine.Option(names = {"-force", "--force"})
    private boolean force;

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionsShareDownload(items, force);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiConnectionsShareDownloadResponseDTO>() {
        };
    }
}
