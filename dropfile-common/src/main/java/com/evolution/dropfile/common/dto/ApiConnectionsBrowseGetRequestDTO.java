package com.evolution.dropfile.common.dto;

import com.evolution.dropfile.common.CriteriaEnvelope;

public record ApiConnectionsBrowseGetRequestDTO(CriteriaEnvelope fileIdCriteriaEnvelope,
                                                String filename) {
}
