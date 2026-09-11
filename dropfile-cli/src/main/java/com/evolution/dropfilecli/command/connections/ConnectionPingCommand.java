package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    @CommandLine.Parameters(index = "0", description = "Revoke by fingerprint", defaultValue = "")
    private String fingerprintCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        CriteriaEnvelope criteriaEnvelope = StringUtils.hasText(fingerprintCriteria)
                ? new CriteriaEnvelope(fingerprintCriteria)
                : null;
        return daemonClient.connectionsTunnelPing(criteriaEnvelope);
    }
}
