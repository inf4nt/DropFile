package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiConnectionsShareDownloadResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "download",
        description = "Download file"
)
public class ConnectionsShareDownloadCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "File id")
    private String id;

    @CommandLine.Option(names = {"-filename", "--filename"}, description = "Output filename")
    private String filename;

    @CommandLine.Option(names = {"-rewrite", "--rewrite"}, description = "Rewrite already existing file")
    private boolean rewrite;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionsShareDownloadCommand(DaemonClient daemonClient,
                                           ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsShareDownload(id, filename, rewrite);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        ApiConnectionsShareDownloadResponseDTO responseDTO = objectMapper.readValue(
                response.body(),
                ApiConnectionsShareDownloadResponseDTO.class
        );
        PrintReflection.print(responseDTO);
    }
}
