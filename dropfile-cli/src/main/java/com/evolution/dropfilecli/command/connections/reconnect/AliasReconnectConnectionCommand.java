package com.evolution.dropfilecli.command.connections.reconnect;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "alias",
        description = "Reconnect to the current alias or a specified alias",
        customSynopsis = {
                "dropf connections reconnect alias",
                "dropf connections reconnect alias <alias>"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class AliasReconnectConnectionCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "Reconnect via alias", defaultValue = "")
    private String alias;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (StringUtils.hasText(alias)) {
            return daemonClient.handshakeReconnectAlias(alias);
        }
        return daemonClient.handshakeReconnectAliasCurrent();
    }
}