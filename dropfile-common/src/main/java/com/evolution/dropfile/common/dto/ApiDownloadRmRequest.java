package com.evolution.dropfile.common.dto;

import java.util.Set;

public record ApiDownloadRmRequest(Set<String> operations, boolean force) {
}
