package org.axostudio.axonpcs.api.model;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable container for an NPC's equipped items.
 */
public final class NPCEquipment {
    private final Map<NPCEquipmentSlot, ItemStack> items;

    public NPCEquipment(Map<NPCEquipmentSlot, ItemStack> items) {
        if (items == null || items.isEmpty()) {
            this.items = Collections.emptyMap();
        } else {
            this.items = Collections.unmodifiableMap(new EnumMap<>(items));
        }
    }

    public static NPCEquipment empty() {
        return new NPCEquipment(Collections.emptyMap());
    }

    public ItemStack getItem(NPCEquipmentSlot slot) {
        return items.get(slot);
    }

    public Map<NPCEquipmentSlot, ItemStack> asMap() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
