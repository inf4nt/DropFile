package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "info",
        description = "Retrieve info"
)
public class RetrieveInfoCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    public RetrieveInfoCommand(DaemonClient daemonClient,
                               ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.getDaemonInfo();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        DaemonInfoResponseDTO responseDTO = objectMapper.readValue(
                response.body(),
                DaemonInfoResponseDTO.class
        );
        PrintReflection.print(responseDTO);
    }
}
