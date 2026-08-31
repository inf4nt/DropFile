package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfile.common.dto.ApiQuickShareRmResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove quickshare file",
        customSynopsis = {
                "dropfile quickshare rm <ids>",
                "dropfile quickshare rm one two three"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class QuickShareRmCommand extends AbstractCommandHttpHandler<ApiQuickShareRmResponseDTO> {

    @CommandLine.Parameters(index = "0..*", arity = "1..*", split = ",", description = "Quickshare id")
    private Set<String> ids;

    @Override
    protected TypeReference<ApiQuickShareRmResponseDTO> getTypeReference() {
        return new TypeReference<ApiQuickShareRmResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiQuickShareRmResponseDTO response) {
        response.found().forEach((prefix, operationId) -> {
            if (prefix.equals(operationId)) {
                System.out.printf("Removed quickshare: %s%n", operationId);
            } else {
                System.out.printf("Removed quickshare: %s (prefix: '%s')%n", operationId, prefix);
            }
        });

        response.notFound().forEach(prefix ->
                System.err.printf("Error response from daemon: No such quickshare: %s%n", prefix)
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
        return daemonClient.quickShareRm(ids);
    }
}
