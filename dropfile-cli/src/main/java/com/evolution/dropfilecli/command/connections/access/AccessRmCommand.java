package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessRmResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove access key",
        customSynopsis = {
                "dropfile connections access rm <ids>",
                "dropfile connections access rm one two three"
        }
)
public class AccessRmCommand extends AbstractCommandHttpHandler<ApiConnectionsAccessRmResponseDTO> {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Access key ids")
    private Set<String> ids;

    @Override
    protected TypeReference<ApiConnectionsAccessRmResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsAccessRmResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiConnectionsAccessRmResponseDTO object) {
        object.found().forEach((prefix, fullId) -> {
            if (prefix.equals(fullId)) {
                System.out.printf("Removed access key: %s%n", fullId);
            } else {
                System.out.printf("Removed access key: %s (prefix: '%s')%n", fullId, prefix);
            }
        });

        object.notFound().forEach(prefix ->
                System.err.printf("No such access key: %s%n", prefix)
        );

        object.ambiguous().forEach((prefix, matches) ->
                System.err.printf(
                        "Prefix '%s' is ambiguous. Matches: %s%n",
                        prefix,
                        String.join(", ", matches)
                )
        );
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsAccessRm(ids);
    }
}
