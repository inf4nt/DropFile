package com.evolution.dropfile.common.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ApiBatchOperationResult(Map<String, String> found,
                                      Set<String> notFound,
                                      Map<String, List<String>> ambiguous) {
}
