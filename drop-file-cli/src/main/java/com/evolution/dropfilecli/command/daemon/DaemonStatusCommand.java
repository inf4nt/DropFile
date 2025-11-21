package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.client.DaemonClient;
import com.evolution.dropfilecli.command.ConnectionsConnectCommand;
import com.evolution.dropfilecli.command.ConnectionsOnlineCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Get daemon status command",
        subcommands = {
                ConnectionsConnectCommand.class,
                ConnectionsOnlineCommand.class
        }
)
public class DaemonStatusCommand implements Runnable {

    private final DaemonClient daemonClient;

    public DaemonStatusCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public void run() {
        HttpResponse<Void> httpResponse = daemonClient.pingDaemon();
        if (httpResponse.statusCode() == 200) {
            System.out.println("ONLINE");
        } else {
            System.out.println("ERROR");
        }
        System.out.println("Daemon http status: " + httpResponse.statusCode());
    }
}
