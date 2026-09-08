package com.evolution.dropfile.common.dto;

import com.evolution.dropfile.common.CriteriaEnvelope;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ARCHITECTURAL NOTICE — DO NOT REFACTOR TO USE CriteriaEnvelope DIRECTLY IN THIS RECORD!
 * <p>
 * This class acts as a Transport DTO for JSON serialization. It deliberately uses raw String fields
 * instead of domain Value Objects (CriteriaEnvelope) to maintain a clean boundary between API and Domain layers.
 * <p>
 * Why this separation is required:
 * 1. Prevents adding Jackson framework dependencies/annotations into the domain model.
 * 2. Ensures flat, clean JSON keys/arrays (e.g., {"found": {"prefix": "id"}}) instead of
 * nested objects or malformed toString key representations like "CriteriaEnvelope[value=...]".
 * 3. Avoids Jackson deserialization errors (MismatchedInputException) when reading raw JSON strings.
 * <p>
 * Workflow:
 * - Use `ApiBatchOperationResult.of(...)` when building responses from domain objects.
 * - Use `.toCriteria()` to convert back to strongly-typed domain objects (`CriteriaEnvelope`).
 */
public record ApiBatchOperationResult(Map<String, String> found,
                                      Collection<String> notFound,
                                      Map<String, List<String>> ambiguous) {

    public static ApiBatchOperationResult of(Map<CriteriaEnvelope, String> found,
                                             Collection<CriteriaEnvelope> notFound,
                                             Map<CriteriaEnvelope, List<String>> ambiguous) {
        return new ApiBatchOperationResult(
                found.entrySet().stream().collect(Collectors.toMap(
                        x -> x.getKey().value(),
                        Map.Entry::getValue,
                        (_, v2) -> v2,
                        LinkedHashMap::new
                )),
                notFound.stream().map(CriteriaEnvelope::value).toList(),
                ambiguous.entrySet().stream().collect(Collectors.toMap(
                        x -> x.getKey().value(),
                        Map.Entry::getValue,
                        (_, v2) -> v2,
                        LinkedHashMap::new
                ))
        );
    }

    public ApiBatchOperationResultCriteria toCriteria() {
        return new ApiBatchOperationResultCriteria(
                found.entrySet().stream().collect(Collectors.toMap(
                        x -> new CriteriaEnvelope(x.getKey()),
                        Map.Entry::getValue,
                        (_, v2) -> v2,
                        LinkedHashMap::new
                )),
                notFound.stream().map(CriteriaEnvelope::new).toList(),
                ambiguous.entrySet().stream().collect(Collectors.toMap(
                        x -> new CriteriaEnvelope(x.getKey()),
                        Map.Entry::getValue,
                        (_, v2) -> v2,
                        LinkedHashMap::new
                ))
        );
    }


    public record ApiBatchOperationResultCriteria(Map<CriteriaEnvelope, String> found,
                                                  Collection<CriteriaEnvelope> notFound,
                                                  Map<CriteriaEnvelope, List<String>> ambiguous) {
    }
}
