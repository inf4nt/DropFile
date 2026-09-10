package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "ping",
        description = "Perform ping to the current connection",
        customSynopsis = {
                "dropf connections ping"
        }
)
public class ConnectionPingCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsTunnelPing();
    }
}
