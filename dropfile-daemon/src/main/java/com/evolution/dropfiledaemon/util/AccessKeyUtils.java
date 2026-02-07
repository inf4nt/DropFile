package com.evolution.dropfiledaemon.util;

import com.evolution.dropfile.common.CommonUtils;

public class AccessKeyUtils {

    public static AccessKeyEnvelope generate() {
        String key = CommonUtils.generateSecretNonce12();
        String id = getId(key);
        return new AccessKeyEnvelope(id, key);
    }

    public static String getId(String key) {
        return CommonUtils.getFingerprint(key.getBytes()).substring(0, 16);
    }

    public record AccessKeyEnvelope(String id, String key) {
    }
}
