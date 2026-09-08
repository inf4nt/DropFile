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
 * This record serves as a Transport DTO for JSON serialization. It intentionally uses raw String
 * types to maintain a strict separation between API transport and Domain layers.
 * <p>
 * Purpose:
 * 1. Keeps domain model free of Jackson framework dependencies.
 * 2. Prevents key-serialization issues in JSON maps (e.g., prevents "CriteriaEnvelope[value=a]").
 * 3. Guarantees flat JSON output while preserving strong typing within domain logic.
 */
public record ApiDownloadRmResponse(
        Map<String, String> removed,
        Map<String, String> active,
        Collection<String> notFound,
        Map<String, List<String>> ambiguous
) {

    public static ApiDownloadRmResponse of(Map<CriteriaEnvelope, String> removed,
                                           Map<CriteriaEnvelope, String> active,
                                           Collection<CriteriaEnvelope> notFound,
                                           Map<CriteriaEnvelope, List<String>> ambiguous) {
        return new ApiDownloadRmResponse(
                removed.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().value(),
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                )),
                active.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().value(),
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                )),
                notFound.stream().map(CriteriaEnvelope::value).toList(),
                ambiguous.entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().value(),
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ))
        );
    }

    public ApiDownloadRmResponseCriteria toCriteria() {
        return new ApiDownloadRmResponseCriteria(
                removed.entrySet().stream().collect(Collectors.toMap(
                        entry -> new CriteriaEnvelope(entry.getKey()),
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                )),
                active.entrySet().stream().collect(Collectors.toMap(
                        entry -> new CriteriaEnvelope(entry.getKey()),
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                )),
                notFound.stream().map(CriteriaEnvelope::new).toList(),
                ambiguous.entrySet().stream().collect(Collectors.toMap(
                        entry -> new CriteriaEnvelope(entry.getKey()),
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        LinkedHashMap::new
                ))
        );
    }

    public record ApiDownloadRmResponseCriteria(
            Map<CriteriaEnvelope, String> removed,
            Map<CriteriaEnvelope, String> active,
            Collection<CriteriaEnvelope> notFound,
            Map<CriteriaEnvelope, List<String>> ambiguous
    ) {
    }
}