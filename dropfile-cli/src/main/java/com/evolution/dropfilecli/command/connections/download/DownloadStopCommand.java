package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.dto.ApiDownloadStopResponse;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "stop",
        description = "Stop download processes",
        customSynopsis = {
                "dropfile connections download stop <IDS>",
                "dropfile connections download stop one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DownloadStopCommand extends AbstractCommandHttpHandler<ApiDownloadStopResponse> {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Download operation ids")
    private Set<String> ids;

    @Override
    protected TypeReference<ApiDownloadStopResponse> getTypeReference() {
        return new TypeReference<ApiDownloadStopResponse>() {
        };
    }

    @Override
    protected void print(ApiDownloadStopResponse response) {
        response.found().forEach((prefix, operationId) -> {
            if (prefix.equals(operationId)) {
                System.out.printf("Stopped operation: %s%n", operationId);
            } else {
                System.out.printf("Stopped operation: %s (prefix: '%s')%n", operationId, prefix);
            }
        });
        response.notFound().forEach(prefix ->
                System.err.printf("Error response from daemon: No such operation: %s%n", prefix)
        );
        response.ambiguous().forEach((prefix, matches) ->
                System.err.printf(
                        "Error response from daemon: Prefix '%s' is ambiguous. Matches: %s%n",
                        prefix,
                        String.join(", ", matches)
                )
        );
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.downloadStop(ids);
    }
}
