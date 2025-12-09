package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "shutdown",
        description = "Daemon shutdown"
)
public class ShutdownCommand implements CommandHttpHandler<Void> {

    private final DaemonClient daemonClient;

    @Autowired
    public ShutdownCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() throws Exception {
        return daemonClient.shutdown();
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) {
    }
}
