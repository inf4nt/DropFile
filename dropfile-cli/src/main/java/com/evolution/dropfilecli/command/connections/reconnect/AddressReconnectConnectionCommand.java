package com.evolution.dropfilecli.command.connections.reconnect;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "address",
        description = "Reconnect to a specified target address",
        customSynopsis = {
                "dropf connections reconnect address <host>:<port>",
                "dropf connections reconnect address https://dropfile.com"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class AddressReconnectConnectionCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "Reconnect via <host>:<port>")
    private String address;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshakeReconnectAddress(CommonUtils.toURI(address));
    }
}