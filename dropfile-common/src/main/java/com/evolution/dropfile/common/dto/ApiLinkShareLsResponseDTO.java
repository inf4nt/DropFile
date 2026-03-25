package com.evolution.dropfile.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiLinkShareLsResponseDTO(String id,
                                        String fileId,
                                        String secret,
                                        String relative,
                                        List<String> absolute,
                                        boolean used,
                                        Instant updated,
                                        Instant created) {
}
