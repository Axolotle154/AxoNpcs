package org.axostudio.axonpcs.api.model;

import java.util.Objects;

/**
 * Immutable action definition executed when a player interacts with an NPC.
 */
public record NPCAction(String type, String value) {
    public NPCAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        type = type.trim().toUpperCase();
    }
}
