package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Get daemon status command"
)
public class DaemonStatusCommand implements CommandHttpHandler<Void> {

    private final DaemonClient daemonClient;

    @Autowired
    public DaemonStatusCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() {
        return daemonClient.pingDaemon();
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) {
        System.out.println("ONLINE");
    }
}
