package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractApiBatchOperationResultCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove access key",
        customSynopsis = {
                "dropf connections access rm <ids>",
                "dropf connections access rm one two three"
        }
)
public class AccessRmCommand extends AbstractApiBatchOperationResultCommandHttpHandler {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Access key ids")
    private Set<String> accessIdCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsAccessRm(CriteriaEnvelope.map(accessIdCriteria));
    }
}
