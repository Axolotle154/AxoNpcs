package org.axostudio.axonpcs.api.model;

import java.util.Objects;

/**
 * Immutable action definition executed when a player interacts with an NPC.
 */
public final class NPCAction {
    private final String type;
    private final String value;

    public NPCAction(String type, String value) {
        this.type = Objects.requireNonNull(type, "type").trim().toUpperCase();
        this.value = Objects.requireNonNull(value, "value");
    }

    public String type() {
        return type;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof NPCAction)) {
            return false;
        }
        NPCAction that = (NPCAction) object;
        return type.equals(that.type) && value.equals(that.value);
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
