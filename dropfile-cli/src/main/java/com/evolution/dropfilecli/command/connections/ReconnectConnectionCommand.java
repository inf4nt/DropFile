package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "reconnect",
        description = "Reconnect to the current connection and rotate session keys",
        customSynopsis = "dropf connections reconnect",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ReconnectConnectionCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshakeCurrentReconnect();
    }
}
