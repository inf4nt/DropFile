package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.CriteriaEnvelope;
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
    private Set<String> operationIdCriteria;

    @CommandLine.Option(names = {"--force"}, defaultValue = "false")
    private boolean force;

    @Override
    protected TypeReference<ApiDownloadRmResponse> getTypeReference() {
        return new TypeReference<ApiDownloadRmResponse>() {
        };
    }

    @Override
    protected void print(ApiDownloadRmResponse responseDTO) {
        ApiDownloadRmResponse.ApiDownloadRmResponseCriteria criteriaResponse = responseDTO.toCriteria();

        criteriaResponse.removed().forEach((criteria, operationId) -> {
            System.out.printf("Removed operation: %s (prefix: '%s')%n", operationId, criteria.value());
        });

        criteriaResponse.active().forEach((criteria, operationId) -> {
            System.out.printf("Operation %s (prefix: '%s') is active. Use --force to remove%n", operationId, criteria.value());
        });

        criteriaResponse.notFound().forEach(criteria ->
                System.err.printf("Error response from daemon: No such operation: %s%n", criteria.value())
        );

        criteriaResponse.ambiguous().forEach((criteria, matches) ->
                System.err.printf(
                        "Error response from daemon: Prefix '%s' is ambiguous. Matches: %s%n",
                        criteria.value(),
                        String.join(", ", matches)
                )
        );
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsDownloadRm(CriteriaEnvelope.map(operationIdCriteria), force);
    }
}
