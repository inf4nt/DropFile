package com.evolution.dropfilecli.command.connections.reconnect;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "fingerprint",
        description = "Reconnect to the current fingerprint connection or a specified fingerprint",
        customSynopsis = {
                "dropf connections reconnect fingerprint",
                "dropf connections reconnect fingerprint <fingerprint>"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class FingerprintReconnectConnectionCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "Reconnect via fingerprint", defaultValue = "")
    private String fingerprint;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (StringUtils.hasText(fingerprint)) {
            return daemonClient.handshakeReconnectFingerprint(new CriteriaEnvelope(fingerprint));
        }
        return daemonClient.handshakeReconnectCurrent();
    }
}