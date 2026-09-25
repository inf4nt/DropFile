package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        aliases = {"c"},
        description = "Perform connection to the given address",
        customSynopsis = {
                "dropf connections connect <address> <access-key> [options]",
                "dropf connections connect 192.168.1.3:28282 top_secret",
                "dropf connections connect https://drorpfile.test top_secret",
                "dropf connections connect https://drorpfile.test top_secret my_phone"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ConnectCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Access secret key")
    private String accessSecretKey;

    @CommandLine.Parameters(index = "2", description = "Alias", defaultValue = "")
    private String alias;

    @CommandLine.Option(names = {"--force"}, defaultValue = "false")
    private boolean force;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.handshake(CommonUtils.toURI(address), accessSecretKey, alias, force);
    }
}
