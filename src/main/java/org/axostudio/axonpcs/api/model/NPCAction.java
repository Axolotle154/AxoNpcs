package org.axostudio.axonpcs.api.model;

import java.util.Locale;
import java.util.Objects;

/**
 * An action executed upon player interaction with an NPC.
 */
public final class NPCAction {
    private final String type;
    private final String value;

    public NPCAction(String type, String value) {
        this.type = type == null ? "message" : type.trim().toLowerCase(Locale.ROOT);
        this.value = value == null ? "" : value;
    }

    public String type() {
        return type;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NPCAction other)) return false;
        return type.equals(other.type) && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "NPCAction[type=" + type + ", value=" + value + "]";
    }
}
