package com.evolution.dropfilecli.command;

import com.evolution.dropfile.configuration.dto.ConnectionsOnline;
import com.evolution.dropfilecli.CommandHttpHandler;
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
public class ConnectionsOnlineCommand implements CommandHttpHandler<String> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ConnectionsOnlineCommand(DaemonClient daemonClient, ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<String> execute() {
        return daemonClient.getOnlineConnections();
    }

    @SneakyThrows
    @Override
    public void handleSuccessful(HttpResponse<String> response) {
        ConnectionsOnline connectionsOnline = objectMapper.readValue(response.body(), ConnectionsOnline.class);
        List<ConnectionsOnline.Online> onlineList = connectionsOnline.getOnlineList();
        for (ConnectionsOnline.Online online : onlineList) {
            String message = String.format("Status: %s | Connection id: %s | Connection address: %s",
                    online.getStatus(), online.getConnectionId(), online.getConnectionAddress());
            System.out.println(message);
        }

        if (onlineList.isEmpty()) {
            System.out.println("No online connections");
        }
    }
}
