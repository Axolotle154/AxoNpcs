package org.axostudio.axonpcs.api.model;

public enum NPCEquipmentSlot {
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    MAIN_HAND,
    OFF_HAND;

    public static NPCEquipmentSlot parse(String value) {
        return valueOf(value.trim().toUpperCase().replace('-', '_'));
    }
}
