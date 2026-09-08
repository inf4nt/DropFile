package com.evolution.dropfilecli.command;

import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.fasterxml.jackson.core.type.TypeReference;

public abstract class AbstractApiBatchOperationResultCommandHttpHandler extends AbstractCommandHttpHandler<ApiBatchOperationResult> {

    @Override
    protected TypeReference<ApiBatchOperationResult> getTypeReference() {
        return new TypeReference<ApiBatchOperationResult>() {
        };
    }

    @Override
    protected void print(ApiBatchOperationResult result) {
        ApiBatchOperationResult.ApiBatchOperationResultCriteria object = result.toCriteria();

        object.found().forEach((criteria, fullId) -> {
            String value = criteria.value();
            System.out.printf("Removed: %s (criteria: '%s')%n", fullId, value);
        });

        object.notFound().forEach(criteria ->
                System.err.printf("No such element found: %s%n", criteria.value())
        );

        object.ambiguous().forEach((criteria, matches) ->
                System.err.printf(
                        "Prefix '%s' is ambiguous. Matches: %s%n",
                        criteria.value(),
                        String.join(", ", matches)
                )
        );
    }
}
