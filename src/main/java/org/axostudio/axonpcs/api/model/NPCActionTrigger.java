package org.axostudio.axonpcs.api.model;

public enum NPCActionTrigger {
    RIGHT_CLICK,
    LEFT_CLICK,
    ANY;

    public static NPCActionTrigger parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return RIGHT_CLICK;
        }
        switch (value.trim().toUpperCase().replace('-', '_')) {
            case "LEFT":
            case "LEFT_CLICK":
            case "ATTACK":
                return LEFT_CLICK;
            case "ANY":
            case "BOTH":
                return ANY;
            default:
                return RIGHT_CLICK;
        }
    }
}
