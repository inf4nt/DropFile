package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfilecli.command.AbstractApiBatchOperationResultCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove quickshare file",
        customSynopsis = {
                "dropf quickshare rm <ids>",
                "dropf quickshare rm one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class QuickShareRmCommand extends AbstractApiBatchOperationResultCommandHttpHandler {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Quickshare id")
    private Set<String> idCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.quickShareRm(CriteriaEnvelope.map(idCriteria));
    }
}
