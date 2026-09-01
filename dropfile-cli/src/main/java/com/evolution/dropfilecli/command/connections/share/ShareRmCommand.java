package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareRmResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove shared file",
        customSynopsis = {
                "dropfile connections share rm <ids>",
                "dropfile connections share rm one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ShareRmCommand extends AbstractCommandHttpHandler<ApiConnectionsShareRmResponseDTO> {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Share ids")
    private Set<String> ids;

    @Override
    protected TypeReference<ApiConnectionsShareRmResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsShareRmResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiConnectionsShareRmResponseDTO object) {
        object.found().forEach((prefix, fullId) -> {
            if (prefix.equals(fullId)) {
                System.out.printf("Removed shared file: %s%n", fullId);
            } else {
                System.out.printf("Removed shared file: %s (prefix: '%s')%n", fullId, prefix);
            }
        });

        object.notFound().forEach(prefix ->
                System.err.printf("Error response from daemon: No such shared file: %s%n", prefix)
        );

        object.ambiguous().forEach((prefix, matches) ->
                System.err.printf(
                        "Error response from daemon: Prefix '%s' is ambiguous. Matches: %s%n",
                        prefix,
                        String.join(", ", matches)
                )
        );
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsShareRm(ids);
    }
}
