package com.evolution.dropfilecli.command.daemon.config;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "daemon-public-address",
        aliases = {"--spa", "-spa"},
        description = "Set public daemon address"
)
public class SetPublicAddressCommand implements CommandHttpHandler<Void> {

    private final DaemonClient daemonClient;

    @CommandLine.Parameters(index = "0", description = "Public address")
    private String address;

    @Autowired
    public SetPublicAddressCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() throws Exception {
        return daemonClient.setPublicAddress(address);
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {
        System.out.println("OK");
    }
}
