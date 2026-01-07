package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
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
        name = "ls",
        description = "ls shared files"
)
public class ConnectionsShareLsCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionsShareLsCommand(DaemonClient daemonClient,
                                     ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionShareLs();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<ApiConnectionsShareLsResponseDTO> values = objectMapper.readValue(
                response.body(),
                new TypeReference<List<ApiConnectionsShareLsResponseDTO>>() {
                }
        );
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No files found");
        }
    }
}
