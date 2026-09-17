package org.axostudio.axonpcs.api.model;

public enum NPCActionTrigger {
    RIGHT_CLICK,
    LEFT_CLICK,
    ANY;

    public static NPCActionTrigger parse(String raw) {
        if (raw == null || raw.isBlank()) return ANY;
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ANY;
        }
    }
}
