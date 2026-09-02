package com.evolution.dropfile.common.dto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record ApiDownloadRmResponse(
        Map<String, String> removed,
        Map<String, String> active,
        Collection<String> notFound,
        Map<String, List<String>> ambiguous
) {
}
