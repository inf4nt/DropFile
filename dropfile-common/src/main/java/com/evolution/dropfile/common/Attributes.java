package com.evolution.dropfile.common;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Attributes {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public <T> Optional<T> get(String attributeKey, Class<T> attributeValueClass) {
        Objects.requireNonNull(attributeValueClass);
        return get(attributeKey)
                .map(attributeValueClass::cast);
    }

    public Optional<Object> get(String attributeKey) {
        validateAttributeKeyNotBlank(attributeKey);
        return Optional.ofNullable(attributes.get(attributeKey));
    }

    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(String attributeKey, Function<String, ? extends T> mappingFunction) {
        validateAttributeKeyNotBlank(attributeKey);
        return (T) attributes.computeIfAbsent(attributeKey, mappingFunction);
    }

    private void validateAttributeKeyNotBlank(String attributeKey) {
        if (attributeKey == null || attributeKey.isBlank()) {
            throw new IllegalArgumentException("Attribute key cannot be empty string");
        }
    }
}