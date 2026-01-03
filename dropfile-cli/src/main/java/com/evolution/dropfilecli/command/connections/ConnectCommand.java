package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiHandshakeStatusResponseDTO;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        description = "Connect"
)
public class ConnectCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Instance Key", defaultValue = "")
    private String key;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectCommand(DaemonClient daemonClient,
                          ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public void run() {
        if (!ObjectUtils.isEmpty(key)) {
            System.out.println("Connecting...");
            HttpResponse<byte[]> handshakeResponse = daemonClient.handshake(CommonUtils.toURI(address), key);
            if (handshakeResponse.statusCode() == 200) {
                ApiHandshakeStatusResponseDTO responseDTO = objectMapper.readValue(
                        handshakeResponse.body(),
                        ApiHandshakeStatusResponseDTO.class
                );
                PrintReflection.print(responseDTO);
                return;
            }
        } else {
            System.out.println("Reconnecting...");
            HttpResponse<byte[]> handshakeResponse = daemonClient.handshakeReconnect(CommonUtils.toURI(address));
            if (handshakeResponse.statusCode() == 200) {
                ApiHandshakeStatusResponseDTO responseDTO = objectMapper.readValue(
                        handshakeResponse.body(),
                        ApiHandshakeStatusResponseDTO.class
                );
                PrintReflection.print(responseDTO);
                return;
            }
        }

        System.out.println("Failed");
    }
}
