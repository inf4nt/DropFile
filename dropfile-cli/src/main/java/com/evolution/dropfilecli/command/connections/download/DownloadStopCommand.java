package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractApiBatchOperationResultCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "stop",
        description = "Stop download processes",
        customSynopsis = {
                "dropf connections download stop <ids>",
                "dropf connections download stop one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DownloadStopCommand extends AbstractApiBatchOperationResultCommandHttpHandler {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Download operation ids")
    private Set<String> operationIdCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsDownloadStop(CriteriaEnvelope.map(operationIdCriteria));
    }
}
