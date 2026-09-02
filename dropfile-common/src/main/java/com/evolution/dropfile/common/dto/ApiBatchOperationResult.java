package com.evolution.dropfile.common.dto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record ApiBatchOperationResult(Map<String, String> found,
                                      Collection<String> notFound,
                                      Map<String, List<String>> ambiguous) {
}
