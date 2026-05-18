package org.axostudio.axonpcs.api.model;

public enum NPCActionTrigger {
    RIGHT_CLICK,
    LEFT_CLICK,
    ANY;

    public static NPCActionTrigger parse(String value) {
        if (value == null || value.isBlank()) {
            return RIGHT_CLICK;
        }
        return switch (value.trim().toUpperCase().replace('-', '_')) {
            case "LEFT", "LEFT_CLICK", "ATTACK" -> LEFT_CLICK;
            case "ANY", "BOTH" -> ANY;
            default -> RIGHT_CLICK;
        };
    }
}
