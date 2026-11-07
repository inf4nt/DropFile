package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CriteriaEnvelope;
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
                "dropf connections connect <address> --secret <access-key>",
                "dropf connections connect <address> --fingerprint <fingerprint>",
                "dropf connections connect 192.168.1.3:28282 --secret top_secret",
                "dropf connections connect https://drorpfile.test --secret top_secret",
                "dropf connections connect https://drorpfile.test --fingerprint abcdefg"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ConnectCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.ArgGroup
    private Arguments arguments;

    private static class Arguments {
        @CommandLine.Option(names = {"--fingerprint", "-f"}, description = "Treat credential as a fingerprint and add address to existing handshake")
        private String fingerprint;

        @CommandLine.Option(names = {"--secret", "-s"}, description = "Treat credential as a secret key")
        private String secret;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (arguments != null && StringUtils.hasText(arguments.fingerprint)) {
            return daemonClient.handshakeExtend(address, new CriteriaEnvelope(arguments.fingerprint));
        }
        if (arguments != null && StringUtils.hasText(arguments.secret)) {
            return daemonClient.handshake(address, arguments.secret);
        }

        return daemonClient.handshakeExtendCurrent(address);
    }
}
