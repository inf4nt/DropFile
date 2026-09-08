package com.evolution.dropfile.common;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.StreamSupport;

public record CriteriaEnvelope(String value) {

    public CriteriaEnvelope {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Criteria value cannot be an empty string");
        }
    }

    public static Collection<CriteriaEnvelope> map(Iterable<String> criteriaStrings) {
        if (criteriaStrings == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(criteriaStrings.spliterator(), false).map(CriteriaEnvelope::new).toList();
    }
}
