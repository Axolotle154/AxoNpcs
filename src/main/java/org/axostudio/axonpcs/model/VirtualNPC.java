package org.axostudio.axonpcs.model;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VirtualNPC implements AxoNPC {
    private final String id;
    private final UUID uniqueId;
    private final int entityId;
    private volatile boolean enabled = true;
    private volatile String type = "PLAYER";
    private volatile NPCPosition position;
    private volatile String displayName;
    private volatile NPCSkin skin = NPCSkin.none();
    private volatile String glowing = "off";
    private volatile boolean collidable;
    private volatile double scale = 1.0D;
    private volatile double viewDistance = 48.0D;
    private volatile double interactionCooldownSeconds = 1.5D;
    private final Map<NPCEquipmentSlot, ItemStack> equipment = new ConcurrentHashMap<>();
    private final Map<NPCActionTrigger, List<NPCAction>> actions = new ConcurrentHashMap<>();

    public VirtualNPC(String id, UUID uniqueId, int entityId, NPCPosition position) {
        this.id = Objects.requireNonNull(id, "id");
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.entityId = entityId;
        this.position = Objects.requireNonNull(position, "position");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null || type.isBlank() ? "PLAYER" : type.trim().toUpperCase();
    }

    public NPCPosition getPosition() {
        return position;
    }

    public void setPosition(NPCPosition position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    @Override
    public Location getLocation() {
        return position.toLocation();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? null : displayName;
    }

    @Override
    public NPCSkin getSkin() {
        return skin;
    }

    public void setSkin(NPCSkin skin) {
        this.skin = skin == null ? NPCSkin.none() : skin;
    }

    @Override
    public String getGlowing() {
        return glowing;
    }

    public void setGlowing(String glowing) {
        this.glowing = glowing == null || glowing.isBlank() ? "off" : glowing.trim().toLowerCase();
    }

    @Override
    public boolean isCollidable() {
        return collidable;
    }

    public void setCollidable(boolean collidable) {
        this.collidable = collidable;
    }

    @Override
    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.0625D, Math.min(16.0D, scale));
    }

    @Override
    public double getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(double viewDistance) {
        this.viewDistance = Math.max(1.0D, viewDistance);
    }

    @Override
    public double getInteractionCooldownSeconds() {
        return interactionCooldownSeconds;
    }

    public void setInteractionCooldownSeconds(double interactionCooldownSeconds) {
        this.interactionCooldownSeconds = interactionCooldownSeconds;
    }

    @Override
    public Map<NPCEquipmentSlot, ItemStack> getEquipment() {
        return Collections.unmodifiableMap(equipment);
    }

    public void setEquipment(NPCEquipmentSlot slot, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            equipment.remove(slot);
            return;
        }
        equipment.put(slot, itemStack.clone());
    }

    public void clearEquipment() {
        equipment.clear();
    }

    @Override
    public List<NPCAction> getActions(NPCActionTrigger trigger) {
        List<NPCAction> current = actions.get(trigger);
        if (current == null) {
            return List.of();
        }
        return List.copyOf(current);
    }

    public Map<NPCActionTrigger, List<NPCAction>> getActionsByTrigger() {
        Map<NPCActionTrigger, List<NPCAction>> copy = new EnumMap<>(NPCActionTrigger.class);
        for (Map.Entry<NPCActionTrigger, List<NPCAction>> entry : actions.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    public void addAction(NPCActionTrigger trigger, NPCAction action) {
        actions.compute(trigger, (key, current) -> {
            List<NPCAction> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
            next.add(action);
            return next;
        });
    }

    public boolean removeAction(NPCActionTrigger trigger, int index) {
        List<NPCAction> current = actions.get(trigger);
        if (current == null || index < 0 || index >= current.size()) {
            return false;
        }
        List<NPCAction> next = new ArrayList<>(current);
        next.remove(index);
        if (next.isEmpty()) {
            actions.remove(trigger);
        } else {
            actions.put(trigger, next);
        }
        return true;
    }

    public void setActions(NPCActionTrigger trigger, List<NPCAction> values) {
        if (values == null || values.isEmpty()) {
            actions.remove(trigger);
            return;
        }
        actions.put(trigger, new ArrayList<>(values));
    }
}
