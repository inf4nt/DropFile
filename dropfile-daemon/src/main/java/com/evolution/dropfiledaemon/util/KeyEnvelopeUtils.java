package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.CommonUtils;

public class KeyEnvelopeUtils {
    
    public static KeyEnvelope generate() {
        String key = CommonUtils.generateRawSecretNonce12();
        String id = getId(key);
        return new KeyEnvelope(id, key);
    }

    public static String getId(String key) {
        return CommonUtils.getFingerprint(key.getBytes()).substring(0, 10);
    }

    public record KeyEnvelope(String id, String key) {
    }

}
