package org.axostudio.axonpcs.protocol.packet;

import net.kyori.adventure.text.Component;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot;
import org.axostudio.axonpcs.protocol.inbound.InboundInteractPacket;
import org.axostudio.axonpcs.protocol.lookup.MethodHandleTable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Array;
import java.util.*;

public final class NativePacketFactory {
    private final MethodHandleTable table;
    private final Map<EntityType, Object> entityTypeCache = new EnumMap<>(EntityType.class);

    public NativePacketFactory(MethodHandleTable table) {
        this.table = table;
    }

    public MethodHandleTable table() {
        return table;
    }

    public Object createGameProfile(UUID uuid, String name, String textureValue, String textureSignature) throws Throwable {
        Object multimap = table.arrayListMultimapCreate.invoke();
        if (textureValue != null && !textureValue.isBlank()) {
            Object property = table.propertyConstructor.invoke("textures", textureValue, textureSignature == null ? "" : textureSignature);
            // Multimap.put("textures", property)
            multimap.getClass().getMethod("put", Object.class, Object.class).invoke(multimap, "textures", property);
        }
        Object propertyMap = table.propertyMapConstructor.invoke(multimap);
        return table.gameProfileConstructor.invoke(uuid, name, propertyMap);
    }

    public Object playerInfoAdd(UUID uuid, Object gameProfile, Component displayName) throws Throwable {
        Object vanillaName = displayName != null ? table.adventureAsVanilla.invoke(displayName) : null;
        Object entry = table.playerInfoEntryConstructor.invoke(
                uuid,
                gameProfile,
                false, // listed = false (never show in tablist)
                0,
                table.gameTypeCreative,
                vanillaName,
                true,
                0,
                null
        );

        @SuppressWarnings({"rawtypes", "unchecked"})
        EnumSet actions = EnumSet.of(
                (Enum) table.actionAddPlayer,
                (Enum) table.actionUpdateGameMode,
                (Enum) table.actionUpdateListed,
                (Enum) table.actionUpdateLatency,
                (Enum) table.actionUpdateDisplayName,
                (Enum) table.actionUpdateHat
        );

        return table.playerInfoPacketConstructor.invoke(actions, List.of(entry));
    }

    public Object playerInfoRemove(UUID uuid) throws Throwable {
        return table.playerInfoRemoveConstructor.invoke(List.of(uuid));
    }

    public Object addEntity(int entityId, UUID uuid, Location location, EntityType type) throws Throwable {
        Object nmsType = resolveNmsEntityType(type);
        Object vec3Zero = table.vec3Constructor.invoke(0.0D, 0.0D, 0.0D);

        return table.addEntityConstructor.invoke(
                entityId,
                uuid,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                nmsType,
                0,
                vec3Zero,
                (double) location.getYaw()
        );
    }

    public Object removeEntities(int... entityIds) throws Throwable {
        return table.removeEntityConstructor.invoke(entityIds);
    }

    public Object metadata(int entityId, boolean playerLike, boolean living, boolean glowing, Component displayName,
                            boolean soundEnabled, double maxHealth, byte flags) throws Throwable {
        List<Object> values = new ArrayList<>();

        // Entity status flags: index 0 (Byte)
        byte entityFlags = flags;
        if (glowing) {
            entityFlags |= 0x40; // Glowing effect bitmask
        }
        values.add(table.dataValueConstructor.invoke(0, table.byteSerializer, entityFlags));

        // Custom Name: index 2 (Optional<Component>)
        if (displayName != null) {
            Object vanillaComponent = table.adventureAsVanilla.invoke(displayName);
            values.add(table.dataValueConstructor.invoke(2, table.optionalComponentSerializer, Optional.of(vanillaComponent)));
            // Custom Name Visible: index 3 (Boolean)
            values.add(table.dataValueConstructor.invoke(3, table.booleanSerializer, true));
        }

        // Silent: index 4 (Boolean)
        if (!soundEnabled) {
            values.add(table.dataValueConstructor.invoke(4, table.booleanSerializer, true));
        }

        // Living entity health. The index is inherited and changes between
        // protocol revisions, so never send it to non-living entity types.
        int healthIndex = table.capabilities.livingHealthDataWatcherIndex();
        if (living && healthIndex >= 0 && maxHealth > 0.0D) {
            values.add(table.dataValueConstructor.invoke(healthIndex, table.floatSerializer, (float) maxHealth));
        }

        // Player skin layers: index 17 (or dynamic detected) -> 0x7F enables all skin parts (cape, jacket, sleeves, pants, hat)
        if (playerLike && table.capabilities.skinCustomizationDataWatcherIndex() >= 0) {
            values.add(table.dataValueConstructor.invoke(table.capabilities.skinCustomizationDataWatcherIndex(), table.byteSerializer, (byte) 0x7F));
        }

        return table.metadataConstructor.invoke(entityId, values);
    }

    public Object attributes(int entityId, double scale, double maxHealth) throws Throwable {
        List<Object> snapshots = new ArrayList<>();
        if (table.scaleAttribute != null && Double.isFinite(scale) && scale > 0.0D) {
            snapshots.add(table.attributeSnapshotConstructor.invoke(
                    table.scaleAttribute, scale, Collections.emptyList()
            ));
        }
        if (table.maxHealthAttribute != null && Double.isFinite(maxHealth) && maxHealth > 0.0D) {
            snapshots.add(table.attributeSnapshotConstructor.invoke(
                    table.maxHealthAttribute, maxHealth, Collections.emptyList()
            ));
        }
        return snapshots.isEmpty() ? null : table.updateAttributesConstructor.invoke(entityId, snapshots);
    }

    public Object createArmorStandLabel(int entityId, UUID uuid, Location location, Component line) throws Throwable {
        Object nmsArmorStandType = resolveNmsEntityType(EntityType.ARMOR_STAND);
        Object vec3Zero = table.vec3Constructor.invoke(0.0D, 0.0D, 0.0D);

        return table.addEntityConstructor.invoke(
                entityId,
                uuid,
                location.getX(),
                location.getY(),
                location.getZ(),
                0.0F,
                0.0F,
                nmsArmorStandType,
                0,
                vec3Zero,
                0.0D
        );
    }

    public Object labelMetadata(int entityId, Component line) throws Throwable {
        List<Object> values = new ArrayList<>();
        // Invisible flag: bit 0x20 on index 0
        values.add(table.dataValueConstructor.invoke(0, table.byteSerializer, (byte) 0x20));

        if (line != null) {
            Object vanillaComponent = table.adventureAsVanilla.invoke(line);
            values.add(table.dataValueConstructor.invoke(2, table.optionalComponentSerializer, Optional.of(vanillaComponent)));
            values.add(table.dataValueConstructor.invoke(3, table.booleanSerializer, true));
        }

        // No gravity: index 5 (Boolean)
        values.add(table.dataValueConstructor.invoke(5, table.booleanSerializer, true));

        // Armor stand client flags: small (0x01) + marker (0x10). This moved
        // from legacy index 15 to 14 in 26.2, so use the runtime accessor.
        int armorStandFlagsIndex = table.capabilities.armorStandClientFlagsDataWatcherIndex();
        if (armorStandFlagsIndex >= 0) {
            values.add(table.dataValueConstructor.invoke(armorStandFlagsIndex, table.byteSerializer, (byte) 0x11));
        }

        return table.metadataConstructor.invoke(entityId, values);
    }

    public Object equipment(int entityId, Map<NPCEquipmentSlot, ItemStack> equipment) throws Throwable {
        List<Object> pairs = new ArrayList<>();
        for (NPCEquipmentSlot slot : NPCEquipmentSlot.values()) {
            ItemStack stack = equipment != null ? equipment.get(slot) : null;
            if (stack == null) {
                stack = new ItemStack(Material.AIR);
            }
            Object nmsItem = table.craftItemAsNmsCopy.invoke(stack);
            Object nmsSlot = table.nmsEquipmentSlots[slot.slotId()];
            pairs.add(table.pairOf.invoke(nmsSlot, nmsItem));
        }
        return table.equipmentPacketConstructor.invoke(entityId, pairs);
    }

    public Object bodyRotation(int entityId, float yaw, float pitch) throws Throwable {
        return table.bodyRotationConstructor.invoke(entityId, packAngle(yaw), packAngle(pitch), true);
    }

    public Object headRotation(int entityId, float yaw) throws Throwable {
        if (table.headRotationConstructor != null
                && table.headRotationConstructor.type().parameterCount() == 2
                && table.headRotationConstructor.type().parameterType(0) == int.class) {
            return table.headRotationConstructor.invoke(entityId, packAngle(yaw));
        }

        if (table.headRotationEntityIdSetter == null || table.headRotationValueSetter == null) {
            return null;
        }

        Object packet;
        if (table.headRotationConstructor != null && table.headRotationConstructor.type().parameterCount() == 0) {
            packet = table.headRotationConstructor.invoke();
        } else if (table.headRotationAllocator != null) {
            packet = table.headRotationAllocator.invoke();
        } else {
            return null;
        }

        table.headRotationEntityIdSetter.invoke(packet, entityId);
        table.headRotationValueSetter.invoke(packet, packAngle(yaw));
        return packet;
    }

    public Object teamCreate(String teamName, String entry, boolean collision, String colorName) throws Throwable {
        Object scoreboard = table.scoreboardConstructor.invoke();
        Object team = table.playerTeamConstructor.invoke(scoreboard, teamName);

        table.teamSetVisibility.invoke(team, table.teamVisibilityNever);

        if (!collision && table.teamSetCollision != null && table.teamCollisionNever != null) {
            table.teamSetCollision.invoke(team, table.teamCollisionNever);
        }

        if (table.teamSetColor != null && table.chatFormattingClass != null && colorName != null) {
            String normalizedColor = colorName.trim().toUpperCase(Locale.ROOT);
            if (!normalizedColor.equals("OFF") && !normalizedColor.isBlank()) {
                try {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    Object color = Enum.valueOf((Class<? extends Enum>) table.chatFormattingClass, normalizedColor);
                    table.teamSetColor.invoke(team, color);
                } catch (IllegalArgumentException ignored) {
                    // Invalid colors keep the vanilla default instead of breaking the spawn.
                }
            }
        }

        table.scoreboardAddPlayerToTeam.invoke(scoreboard, entry, team);
        return table.teamCreatePacket.invoke(team, true);
    }

    public Object teamRemove(String teamName) throws Throwable {
        Object scoreboard = table.scoreboardConstructor.invoke();
        Object team = table.playerTeamConstructor.invoke(scoreboard, teamName);
        return table.teamRemovePacket.invoke(team);
    }

    public InboundInteractPacket decodeInbound(Object packet) {
        try {
            if (table.attackPacketClass != null && table.attackPacketClass.isInstance(packet)) {
                if (table.attackEntityIdGetter != null) {
                    int entityId = (int) table.attackEntityIdGetter.invoke(packet);
                    return new InboundInteractPacket(entityId, NPCActionTrigger.LEFT_CLICK);
                }
            }
            if (table.interactPacketClass.isInstance(packet)) {
                int entityId = (int) table.interactEntityIdGetter.invoke(packet);
                NPCActionTrigger trigger = NPCActionTrigger.RIGHT_CLICK;
                if (table.interactIsAttackMethod != null) {
                    boolean isAttack = (boolean) table.interactIsAttackMethod.invoke(packet);
                    if (isAttack) trigger = NPCActionTrigger.LEFT_CLICK;
                } else if (table.interactActionGetter != null && table.interactActionTypeGetter != null) {
                    Object action = table.interactActionGetter.invoke(packet);
                    if (action != null) {
                        Object type = table.interactActionTypeGetter.invoke(action);
                        if (type != null && type.toString().equalsIgnoreCase("ATTACK")) {
                            trigger = NPCActionTrigger.LEFT_CLICK;
                        }
                    }
                }
                return new InboundInteractPacket(entityId, trigger);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object resolveNmsEntityType(EntityType type) throws Throwable {
        Object cached = entityTypeCache.get(type);
        if (cached != null) return cached;

        if (table.craftEntityTypeBukkitToMinecraft != null) {
            Object nms = table.craftEntityTypeBukkitToMinecraft.invoke(type);
            if (nms != null) {
                entityTypeCache.put(type, nms);
                return nms;
            }
        }

        // Fallback lookup via the named constants holder. In 26.2 these fields
        // moved from EntityType to EntityTypes.
        try {
            java.lang.reflect.Field field = table.entityTypesClass.getDeclaredField(type.name());
            field.setAccessible(true);
            Object nms = field.get(null);
            entityTypeCache.put(type, nms);
            return nms;
        } catch (Throwable t) {
            // Default to PLAYER when a Bukkit type has no direct NMS constant.
            java.lang.reflect.Field field = table.entityTypesClass.getDeclaredField("PLAYER");
            field.setAccessible(true);
            Object fallback = field.get(null);
            entityTypeCache.put(type, fallback);
            return fallback;
        }
    }

    private static byte packAngle(float angle) {
        return (byte) (int) (angle * 256.0F / 360.0F);
    }
}
