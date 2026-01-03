package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.AccessKeyInfoResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "generate",
        description = "Generate access key command"
)
public class AccessGenerateCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public AccessGenerateCommand(DaemonClient daemonClient, ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.generateAccessKeys(false);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        AccessKeyInfoResponseDTO value = objectMapper
                .readValue(
                        response.body(),
                        AccessKeyInfoResponseDTO.class
                );
        PrintReflection.print(value);
    }
}
