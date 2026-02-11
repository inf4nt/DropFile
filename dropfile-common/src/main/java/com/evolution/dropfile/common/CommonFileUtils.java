package com.evolution.dropfile.common;

public class CommonFileUtils {

    public static String getTemporaryFileName(String filename) {
        return String.format("Unconfirmed-%s-%s.dropfile", CommonUtils.random(), filename);
    }
}
