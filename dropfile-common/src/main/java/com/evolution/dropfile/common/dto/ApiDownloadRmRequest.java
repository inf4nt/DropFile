package com.evolution.dropfile.common.dto;

import java.util.Collection;

public record ApiDownloadRmRequest(Collection<String> operations, boolean force) {
}
