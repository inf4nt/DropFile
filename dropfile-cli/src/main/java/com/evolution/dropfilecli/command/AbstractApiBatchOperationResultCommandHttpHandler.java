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
            String message = "Processed: %s (criteria: '%s')".formatted(fullId, value);
            System.out.println(message);
        });

        object.notFound().forEach(criteria -> {
            String message = "No such element found: %s".formatted(criteria.value());
            System.err.println(message);
        });

        object.ambiguous().forEach((criteria, matches) -> {
            String message = "Criteria '%s' is ambiguous. Matches: %s. Please provide a longer criteria or full identifier".formatted(
                    criteria.value(),
                    String.join(", ", matches)
            );
            System.err.println(message);
        });
    }
}
