package com.evolution.dropfilecli.command.connections.files;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.LsFileResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls",
        description = "Retrieve files"
)
public class ConnectionsLsFilesCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionsLsFilesCommand(DaemonClient daemonClient,
                                     ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getConnectionFiles();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<LsFileResponseDTO.LsFileEntry> values = objectMapper
                .readValue(
                        response.body(),
                        LsFileResponseDTO.class
                )
                .entries();
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No files found");
        }
    }
}
