package com.evolution.dropfilecli.command;

import com.evolution.dropfile.common.dto.ConnectionsOnline;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "online",
        description = "Get connections statuses"
)
public class ConnectionsOnlineCommand implements Runnable {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionsOnlineCommand(DaemonClient daemonClient, ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public void run() {
        HttpResponse<String> httpResponseOnlineConnections = daemonClient.getOnlineConnections();
        if (httpResponseOnlineConnections.statusCode() == 200) {
            ConnectionsOnline connectionsOnline = objectMapper.readValue(httpResponseOnlineConnections.body(), ConnectionsOnline.class);
            List<ConnectionsOnline.Online> onlineList = connectionsOnline.getOnlineList();

            for (ConnectionsOnline.Online online : onlineList) {
                String message = String.format("Status: %s | Connection id: %s | Connection address: %s",
                        online.getStatus(), online.getConnectionId(), online.getConnectionAddress());
                System.out.println(message);
            }

            if (onlineList.isEmpty()) {
                System.out.println("No online connections");
            }
        } else {
            System.out.println("HTTP response code: " + httpResponseOnlineConnections.statusCode());
        }
    }
}
