package org.axostudio.axonpcs.api.model;

import org.bukkit.inventory.EquipmentSlot;

public enum NPCEquipmentSlot {
    MAIN_HAND(EquipmentSlot.HAND, 0),
    OFF_HAND(EquipmentSlot.OFF_HAND, 5),
    BOOTS(EquipmentSlot.FEET, 1),
    LEGGINGS(EquipmentSlot.LEGS, 2),
    CHESTPLATE(EquipmentSlot.CHEST, 3),
    HELMET(EquipmentSlot.HEAD, 4);

    private final EquipmentSlot bukkitSlot;
    private final int slotId;

    NPCEquipmentSlot(EquipmentSlot bukkitSlot, int slotId) {
        this.bukkitSlot = bukkitSlot;
        this.slotId = slotId;
    }

    public EquipmentSlot toBukkit() {
        return bukkitSlot;
    }

    public int slotId() {
        return slotId;
    }

    public static NPCEquipmentSlot fromBukkit(EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> MAIN_HAND;
            case OFF_HAND -> OFF_HAND;
            case FEET -> BOOTS;
            case LEGS -> LEGGINGS;
            case CHEST -> CHESTPLATE;
            case HEAD -> HELMET;
            default -> MAIN_HAND;
        };
    }

    public static NPCEquipmentSlot parse(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "MAIN_HAND", "MAINHAND", "HAND" -> MAIN_HAND;
            case "OFF_HAND", "OFFHAND" -> OFF_HAND;
            case "BOOTS", "FEET" -> BOOTS;
            case "LEGGINGS", "LEGS" -> LEGGINGS;
            case "CHESTPLATE", "CHEST" -> CHESTPLATE;
            case "HELMET", "HEAD" -> HELMET;
            default -> null;
        };
    }
}
