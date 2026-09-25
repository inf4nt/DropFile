package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "reconnect",
        description = "Reconnect to the current connection or a specified target",
        customSynopsis = {
                "dropf connections reconnect",
                "dropf connections reconnect --fingerprint <fingerprint>",
                "dropf connections reconnect --alias <alias>",
                "dropf connections reconnect --address <host>:<port>"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ReconnectConnectionCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "0..1")
    private Arguments arguments;

    private static class Arguments {

        @CommandLine.Option(
                names = {"--fingerprint", "-f"},
                description = "Reconnect via fingerprint"
        )
        private String fingerprint;

        @CommandLine.Option(
                names = {"--alias"},
                description = "Reconnect via alias"
        )
        private String alias;

        @CommandLine.Option(
                names = {"--address"},
                description = "Reconnect via <host>:<port>"
        )
        private String address;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (arguments == null) {
            return daemonClient.handshakeReconnectCurrent();
        }

        if (StringUtils.hasText(arguments.fingerprint)) {
            return daemonClient.handshakeReconnectFingerprint(new CriteriaEnvelope(arguments.fingerprint));
        }

        if (StringUtils.hasText(arguments.alias)) {
            return daemonClient.handshakeReconnectAlias(new CriteriaEnvelope(arguments.alias));
        }

        if (StringUtils.hasText(arguments.address)) {
            return daemonClient.handshakeReconnectAddress(CommonUtils.toURI(arguments.address));
        }

        throw new IllegalArgumentException("Reconnect target is not specified");
    }
}