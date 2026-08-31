package com.evolution.dropfile.common.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ApiDownloadRmResponse(
        Map<String, String> removed,
        Map<String, String> active,
        Set<String> notFound,
        Map<String, List<String>> ambiguous
) {
}
