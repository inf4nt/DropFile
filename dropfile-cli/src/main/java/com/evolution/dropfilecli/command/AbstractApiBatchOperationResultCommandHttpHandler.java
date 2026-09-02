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
    protected void print(ApiBatchOperationResult object) {
        object.found().forEach((prefix, fullId) -> {
            if (prefix.equals(fullId)) {
                System.out.printf("Removed: %s%n", fullId);
            } else {
                System.out.printf("Removed: %s (prefix: '%s')%n", fullId, prefix);
            }
        });

        object.notFound().forEach(prefix ->
                System.err.printf("No such element found: %s%n", prefix)
        );

        object.ambiguous().forEach((prefix, matches) ->
                System.err.printf(
                        "Prefix '%s' is ambiguous. Matches: %s%n",
                        prefix,
                        String.join(", ", matches)
                )
        );
    }
}
