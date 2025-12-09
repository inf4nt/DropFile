package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.dto.HandshakeApiRequestResponseStatus;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        description = "Connect"
)
public class ConnectCommand implements CommandHttpHandler<String> {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Timeout in seconds. Default value 60sec", defaultValue = "60")
    private Integer timeout;

    private final DaemonClient daemonClient;

    @Autowired
    public ConnectCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<String> execute() throws Exception {
        if (timeout <= 1) {
            return daemonClient.handshakeRequest(address);
        }

        int count = 1;
        HttpResponse<String> retry = null;
        while (count <= timeout) {
            System.out.println(String.format("Attempt %s to connect %s", count, address));
            retry = daemonClient.handshakeRequest(address);
            if (HandshakeApiRequestResponseStatus.SUCCESS.name().equals(retry.body())) {
                break;
            }
            count++;
            Thread.sleep(1000);
        }
        return retry;
    }

    @Override
    public void handleSuccessful(HttpResponse<String> response) throws Exception {
        System.out.println("Status: " + response.body());
    }
}
