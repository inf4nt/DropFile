package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.AddFileRequestDTO;

import java.util.HashMap;
import java.util.Map;

public class FilesStore {
    public static final Map<String, AddFileRequestDTO> STORE = new HashMap<>();
}
