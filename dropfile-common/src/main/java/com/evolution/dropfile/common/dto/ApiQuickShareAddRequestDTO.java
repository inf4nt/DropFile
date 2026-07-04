package com.evolution.dropfile.common.dto;

import java.io.File;

public record ApiQuickShareAddRequestDTO(File file, String alias, boolean singleUse, boolean secure) {
}
