package com.evolution.dropfilecli.command.connections.files;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiConnectionsDownloadFileResponseDTO;
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
public class ConnectionsDownloadFilesCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "File id")
    private String id;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionsDownloadFilesCommand(DaemonClient daemonClient,
                                           ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsDownloadFile(id);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        ApiConnectionsDownloadFileResponseDTO responseDTO = objectMapper.readValue(
                response.body(),
                ApiConnectionsDownloadFileResponseDTO.class
        );
        PrintReflection.print(responseDTO);
    }
}
