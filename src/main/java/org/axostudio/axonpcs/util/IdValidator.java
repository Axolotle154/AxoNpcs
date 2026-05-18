package org.axostudio.axonpcs.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class IdValidator {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,48}");

    private IdValidator() {
    }

    public static boolean isValid(String id) {
        return id != null && SAFE_ID.matcher(id).matches();
    }

    public static String normalize(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public static String requireValid(String id) {
        String normalized = normalize(id);
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("Unsafe NPC id: " + id);
        }
        return normalized;
    }
}
