package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.dto.ApiDownloadRmResponse;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove download process",
        customSynopsis = {
                "dropf connections download rm <ids>",
                "dropf connections download rm one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DownloadRmCommand extends AbstractCommandHttpHandler<ApiDownloadRmResponse> {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Download operation ids")
    private Set<String> ids;

    @CommandLine.Option(names = {"--force"}, defaultValue = "false")
    private boolean force;

    @Override
    protected TypeReference<ApiDownloadRmResponse> getTypeReference() {
        return new TypeReference<ApiDownloadRmResponse>() {
        };
    }

    @Override
    protected void print(ApiDownloadRmResponse response) {
        response.removed().forEach((prefix, operationId) -> {
            if (prefix.equals(operationId)) {
                System.out.printf("Removed operation: %s%n", operationId);
            } else {
                System.out.printf("Removed operation: %s (prefix: '%s')%n", operationId, prefix);
            }
        });

        response.active().forEach((prefix, operationId) -> {
            if (prefix.equals(operationId)) {
                System.out.printf("Operation %s is active. Use --force to remove%n", operationId);
            } else {
                System.out.printf("Operation %s (prefix: '%s') is active. Use --force to remove%n", operationId, prefix);
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
        return daemonClient.connectionsDownloadRm(ids, force);
    }
}
