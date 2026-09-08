package com.evolution.dropfile.common.dto;

import com.evolution.dropfile.common.CriteriaEnvelope;

import java.util.Collection;

public record ApiDownloadRmRequest(Collection<CriteriaEnvelope> operationIdCriteriaEnvelopes, boolean force) {
}
