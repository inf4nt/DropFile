package com.evolution.dropfile.common;

import java.util.UUID;

public class CommonFileUtils {

    public static String getTemporaryFileName(String filename) {
        String prefix = UUID.randomUUID().toString().replace("-", "");
        return String.format("Unconfirmed-%s-%s.dropfile", prefix, filename);
    }
}
