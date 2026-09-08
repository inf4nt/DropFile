package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractApiBatchOperationResultCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "kill",
        description = "Kill(stop and remove) download processes",
        customSynopsis = {
                "dropf connections download kill <ids>",
                "dropf connections download kill one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DownloadKillCommand extends AbstractApiBatchOperationResultCommandHttpHandler {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Download operation ids")
    private Set<String> operationIdCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsDownloadKill(CriteriaEnvelope.map(operationIdCriteria));
    }
}
