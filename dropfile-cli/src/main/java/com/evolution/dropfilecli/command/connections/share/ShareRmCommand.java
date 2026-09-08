package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractApiBatchOperationResultCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove shared file",
        customSynopsis = {
                "dropf connections share rm <ids>",
                "dropf connections share rm one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ShareRmCommand extends AbstractApiBatchOperationResultCommandHttpHandler {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Share ids")
    private Set<String> shareFileIdCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsShareRm(CriteriaEnvelope.map(shareFileIdCriteria));
    }
}
