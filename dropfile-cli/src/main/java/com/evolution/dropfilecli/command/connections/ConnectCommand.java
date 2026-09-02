package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        aliases = {"c"},
        description = "Perform connection to the given address",
        customSynopsis = {
                "dropfile connections connect <address> <access-key> [options]",
                "dropfile connections connect 192.168.1.3:28282 top_secret"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ConnectCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Secret connection key", defaultValue = "")
    private String key;

    @CommandLine.Option(names = {"--force"}, defaultValue = "false")
    private boolean force;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (StringUtils.hasText(key)) {
            return daemonClient.handshake(CommonUtils.toURI(address), key, force);
        }
        return daemonClient.handshakeReconnect(CommonUtils.toURI(address));
    }
}
