package com.evolution.dropfiledaemon.tunnel.command.dto;

import com.evolution.dropfile.common.CriteriaEnvelope;

import java.util.Collection;

public record ShareLsTunnelRequest(Collection<CriteriaEnvelope> criteriaEnvelopes) {
}
