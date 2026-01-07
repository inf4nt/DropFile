package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "cat",
        description = "Cat file"
)
public class ConnectionsShareCatCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "File id")
    private String id;

    private final DaemonClient daemonClient;

    public ConnectionsShareCatCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsCatShareFile(id);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        String responseBody = new String(response.body());
        System.out.println(responseBody);
    }
}
