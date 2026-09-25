package com.evolution.dropfiledaemon.handshake.store.api;

import java.util.regex.Pattern;

public final class AliasValidator {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-z0-9._-]{1,25}$");

    public static boolean isValid(String alias) {
        if (alias == null) {
            return false;
        }
        return ALIAS_PATTERN.matcher(alias).matches();
    }

    public static void validateOrThrow(String alias) {
        if (!isValid(alias)) {
            throw new IllegalArgumentException(
                    "Invalid alias: '" + alias + "'. Alias must be 1-25 characters long, " +
                            "contain only lowercase letters (a-z), numbers (0-9), dots (.), hyphens (-), or underscores (_)."
            );
        }
    }
}