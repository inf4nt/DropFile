package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "disconnect",
        description = "Disconnect trusted-out connection",
        customSynopsis = "dropf connections disconnect <fingerprint>",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DisconnectCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Arguments arguments;

    private static class Arguments {
        @CommandLine.Parameters(index = "0", description = "Disconnect by fingerprint")
        private String fingerprintCriteria;

        @CommandLine.Option(names = {"--current"}, description = "Disconnect current")
        private boolean current;

        @CommandLine.Option(names = {"--all"}, description = "Disconnect all connections")
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (arguments.all) {
            return daemonClient.handshakeDisconnectAll();
        } else if (arguments.current) {
            return daemonClient.handshakeDisconnectCurrent();
        }
        return daemonClient.handshakeDisconnect(new CriteriaEnvelope(arguments.fingerprintCriteria));
    }
}
