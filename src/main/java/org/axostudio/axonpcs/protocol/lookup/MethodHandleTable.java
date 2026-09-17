package org.axostudio.axonpcs.protocol.lookup;

import net.kyori.adventure.text.Component;
import org.axostudio.axonpcs.protocol.adapter.ProtocolCapabilities;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;

public final class MethodHandleTable {
    // Network channel & send
    public final MethodHandle craftPlayerGetHandle;
    public final MethodHandle serverPlayerConnectionGetter;
    public final MethodHandle connectionConnectionGetter;
    public final MethodHandle connectionChannelGetter;
    public final MethodHandle sendPacketMethod;

    // GameProfile & Properties
    public final Class<?> gameProfileClass;
    public final MethodHandle gameProfileConstructor;
    public final MethodHandle propertyConstructor;
    public final MethodHandle arrayListMultimapCreate;
    public final MethodHandle propertyMapConstructor;

    // Adventure
    public final MethodHandle adventureAsVanilla;

    // PlayerInfo
    public final MethodHandle playerInfoEntryConstructor;
    public final MethodHandle playerInfoPacketConstructor;
    public final MethodHandle playerInfoRemoveConstructor;
    public final Object gameTypeCreative;
    public final Object actionAddPlayer;
    public final Object actionUpdateGameMode;
    public final Object actionUpdateListed;
    public final Object actionUpdateLatency;
    public final Object actionUpdateDisplayName;
    public final Object actionUpdateHat;

    // Entities & Math
    public final Class<?> entityTypeClass;
    public final Class<?> entityTypesClass;
    public final MethodHandle craftEntityTypeBukkitToMinecraft;
    public final MethodHandle vec3Constructor;
    public final MethodHandle addEntityConstructor;
    public final MethodHandle removeEntityConstructor;
    public final MethodHandle bodyRotationConstructor;
    public final MethodHandle positionRotationConstructor;
    public final MethodHandle headRotationConstructor;
    public final MethodHandle headRotationAllocator;
    public final MethodHandle headRotationEntityIdSetter;
    public final MethodHandle headRotationValueSetter;

    // Metadata & DataValues
    public final MethodHandle dataValueConstructor;
    public final MethodHandle metadataConstructor;
    public final Object byteSerializer;
    public final Object booleanSerializer;
    public final Object floatSerializer;
    public final Object optionalComponentSerializer;
    public final Object poseSerializer;
    public final Class<?> poseClass;

    // Equipment
    public final MethodHandle equipmentPacketConstructor;
    public final MethodHandle craftItemAsNmsCopy;
    public final MethodHandle pairOf;
    public final Object[] nmsEquipmentSlots;

    // Teams
    public final Class<?> scoreboardClass;
    public final Class<?> playerTeamClass;
    public final MethodHandle scoreboardConstructor;
    public final MethodHandle playerTeamConstructor;
    public final MethodHandle teamSetVisibility;
    public final MethodHandle teamSetCollision;
    public final MethodHandle teamSetColor;
    public final MethodHandle scoreboardAddPlayerToTeam;
    public final MethodHandle teamCreatePacket;
    public final MethodHandle teamRemovePacket;
    public final Object teamVisibilityNever;
    public final Object teamCollisionNever;
    public final Class<?> chatFormattingClass;

    // Attributes
    public final MethodHandle attributeSnapshotConstructor;
    public final MethodHandle updateAttributesConstructor;
    public final Object scaleAttribute;
    public final Object maxHealthAttribute;

    // Serverbound Interact / Attack
    public final Class<?> attackPacketClass;
    public final Class<?> interactPacketClass;
    public final MethodHandle attackEntityIdGetter;
    public final MethodHandle interactEntityIdGetter;
    public final MethodHandle interactIsAttackMethod;
    public final MethodHandle interactActionGetter;
    public final MethodHandle interactActionTypeGetter;

    // Capabilities
    public final ProtocolCapabilities capabilities;

    public MethodHandleTable() throws Exception {
        Class<?> craftPlayerClass = PacketResolver.type("org.bukkit.craftbukkit.entity.CraftPlayer");
        this.craftPlayerGetHandle = PacketResolver.method(craftPlayerClass, "getHandle", PacketResolver.type("net.minecraft.server.level.ServerPlayer"));
        
        Class<?> serverPlayerClass = PacketResolver.type("net.minecraft.server.level.ServerPlayer");
        this.serverPlayerConnectionGetter = PacketResolver.getter(serverPlayerClass, "connection");
        
        Class<?> serverCommonPacketListenerImplClass = PacketResolver.type("net.minecraft.server.network.ServerCommonPacketListenerImpl");
        this.connectionConnectionGetter = PacketResolver.getter(serverCommonPacketListenerImplClass, "connection");
        
        Class<?> connectionClass = PacketResolver.type("net.minecraft.network.Connection");
        this.connectionChannelGetter = PacketResolver.getter(connectionClass, "channel");
        
        Class<?> packetClass = PacketResolver.type("net.minecraft.network.protocol.Packet");
        this.sendPacketMethod = PacketResolver.method(serverCommonPacketListenerImplClass, "send", void.class, packetClass);

        // GameProfile
        this.gameProfileClass = PacketResolver.type("com.mojang.authlib.GameProfile");
        Class<?> propertyMapClass = PacketResolver.type("com.mojang.authlib.properties.PropertyMap");
        Class<?> propertyClass = PacketResolver.type("com.mojang.authlib.properties.Property");
        Class<?> multimapClass = PacketResolver.type("com.google.common.collect.Multimap");
        this.gameProfileConstructor = PacketResolver.constructor(gameProfileClass, UUID.class, String.class, propertyMapClass);
        this.propertyConstructor = PacketResolver.constructor(propertyClass, String.class, String.class, String.class);
        this.arrayListMultimapCreate = PacketResolver.method(PacketResolver.type("com.google.common.collect.ArrayListMultimap"), "create", PacketResolver.type("com.google.common.collect.ArrayListMultimap"));
        this.propertyMapConstructor = PacketResolver.constructor(propertyMapClass, multimapClass);

        // Adventure
        Class<?> paperAdventureClass = PacketResolver.type("io.papermc.paper.adventure.PaperAdventure");
        Class<?> nmsComponentClass = PacketResolver.type("net.minecraft.network.chat.Component");
        this.adventureAsVanilla = PacketResolver.method(paperAdventureClass, "asVanilla", nmsComponentClass, Component.class);

        // PlayerInfo
        Class<?> playerInfoPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        Class<?> playerInfoEntryClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
        Class<?> playerInfoActionClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
        Class<?> gameTypeClass = PacketResolver.type("net.minecraft.world.level.GameType");
        Class<?> chatSessionDataClass = PacketResolver.type("net.minecraft.network.chat.RemoteChatSession$Data");

        this.playerInfoEntryConstructor = PacketResolver.constructor(
                playerInfoEntryClass,
                UUID.class, gameProfileClass, boolean.class, int.class, gameTypeClass, nmsComponentClass, boolean.class, int.class, chatSessionDataClass
        );
        this.playerInfoPacketConstructor = PacketResolver.constructor(playerInfoPacketClass, EnumSet.class, List.class);
        this.playerInfoRemoveConstructor = PacketResolver.constructor(
                PacketResolver.type("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket"), List.class
        );

        this.gameTypeCreative = PacketResolver.staticFieldValue(gameTypeClass, "CREATIVE");
        this.actionAddPlayer = PacketResolver.staticFieldValue(playerInfoActionClass, "ADD_PLAYER");
        this.actionUpdateGameMode = PacketResolver.staticFieldValue(playerInfoActionClass, "UPDATE_GAME_MODE");
        this.actionUpdateListed = PacketResolver.staticFieldValue(playerInfoActionClass, "UPDATE_LISTED");
        this.actionUpdateLatency = PacketResolver.staticFieldValue(playerInfoActionClass, "UPDATE_LATENCY");
        this.actionUpdateDisplayName = PacketResolver.staticFieldValue(playerInfoActionClass, "UPDATE_DISPLAY_NAME");
        this.actionUpdateHat = PacketResolver.staticFieldValue(playerInfoActionClass, "UPDATE_HAT");

        // Entities
        this.entityTypeClass = PacketResolver.type("net.minecraft.world.entity.EntityType");
        // Since 26.2 the named constants (PLAYER, ARMOR_STAND, etc.) live in the
        // separate EntityTypes holder rather than on EntityType itself.
        this.entityTypesClass = PacketResolver.optionalType("net.minecraft.world.entity.EntityTypes")
                .orElse(entityTypeClass);
        Class<?> craftEntityTypeClass = PacketResolver.optionalType("org.bukkit.craftbukkit.entity.CraftEntityType").orElse(null);
        this.craftEntityTypeBukkitToMinecraft = craftEntityTypeClass != null
                ? PacketResolver.optionalMethod(craftEntityTypeClass, "bukkitToMinecraft", entityTypeClass, EntityType.class).orElse(null)
                : null;

        Class<?> vec3Class = PacketResolver.type("net.minecraft.world.phys.Vec3");
        this.vec3Constructor = PacketResolver.constructor(vec3Class, double.class, double.class, double.class);

        Class<?> addEntityPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
        this.addEntityConstructor = PacketResolver.constructor(
                addEntityPacketClass,
                int.class, UUID.class, double.class, double.class, double.class, float.class, float.class,
                entityTypeClass, int.class, vec3Class, double.class
        );

        Class<?> removeEntityPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
        this.removeEntityConstructor = PacketResolver.constructor(removeEntityPacketClass, int[].class);

        Class<?> rotPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Rot");
        this.bodyRotationConstructor = PacketResolver.constructor(rotPacketClass, int.class, byte.class, byte.class, boolean.class);

        Optional<Class<?>> posRotClass = PacketResolver.optionalType("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$PosRot");
        this.positionRotationConstructor = posRotClass.flatMap(c ->
                PacketResolver.optionalConstructor(c, int.class, short.class, short.class, short.class, byte.class, byte.class, boolean.class)
        ).orElse(null);

        Class<?> rotateHeadPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
        Optional<MethodHandle> headCtor = PacketResolver.optionalConstructor(rotateHeadPacketClass, int.class, byte.class);
        if (headCtor.isPresent()) {
            this.headRotationConstructor = headCtor.get();
            this.headRotationAllocator = null;
            this.headRotationEntityIdSetter = null;
            this.headRotationValueSetter = null;
        } else {
            Class<?> entityBaseClass = PacketResolver.optionalType("net.minecraft.world.entity.Entity").orElse(null);
            Optional<MethodHandle> noArgsConstructor = PacketResolver.optionalConstructor(rotateHeadPacketClass);
            this.headRotationConstructor = noArgsConstructor.orElseGet(() -> entityBaseClass != null
                    ? PacketResolver.optionalConstructor(rotateHeadPacketClass, entityBaseClass, byte.class).orElse(null)
                    : null);
            this.headRotationAllocator = noArgsConstructor.isEmpty() && this.headRotationConstructor != null
                    ? PacketResolver.allocator(rotateHeadPacketClass)
                    : null;
            this.headRotationEntityIdSetter = PacketResolver.optionalSetter(rotateHeadPacketClass, "entityId", "id").orElse(null);
            this.headRotationValueSetter = PacketResolver.optionalSetter(rotateHeadPacketClass, "yHeadRot").orElse(null);
        }

        // Metadata
        Class<?> dataValueClass = PacketResolver.type("net.minecraft.network.syncher.SynchedEntityData$DataValue");
        Class<?> dataSerializerClass = PacketResolver.type("net.minecraft.network.syncher.EntityDataSerializer");
        this.dataValueConstructor = PacketResolver.constructor(dataValueClass, int.class, dataSerializerClass, Object.class);

        Class<?> metadataPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
        this.metadataConstructor = PacketResolver.constructor(metadataPacketClass, int.class, List.class);

        Class<?> serializersClass = PacketResolver.type("net.minecraft.network.syncher.EntityDataSerializers");
        this.byteSerializer = PacketResolver.staticFieldValue(serializersClass, "BYTE");
        this.booleanSerializer = PacketResolver.staticFieldValue(serializersClass, "BOOLEAN");
        this.floatSerializer = PacketResolver.staticFieldValue(serializersClass, "FLOAT");
        this.optionalComponentSerializer = PacketResolver.staticFieldValue(serializersClass, "OPTIONAL_COMPONENT");
        this.poseSerializer = PacketResolver.optionalStaticFieldValue(serializersClass, "POSE").orElse(null);
        this.poseClass = PacketResolver.optionalType("net.minecraft.world.entity.Pose").orElse(null);

        // Equipment
        Class<?> equipmentPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket");
        this.equipmentPacketConstructor = PacketResolver.constructor(equipmentPacketClass, int.class, List.class);

        Class<?> craftItemStackClass = PacketResolver.type("org.bukkit.craftbukkit.inventory.CraftItemStack");
        Class<?> nmsItemStackClass = PacketResolver.type("net.minecraft.world.item.ItemStack");
        this.craftItemAsNmsCopy = PacketResolver.method(craftItemStackClass, "asNMSCopy", nmsItemStackClass, ItemStack.class);

        Class<?> pairClass = PacketResolver.type("com.mojang.datafixers.util.Pair");
        this.pairOf = PacketResolver.method(pairClass, "of", pairClass, Object.class, Object.class);

        Class<?> equipmentSlotClass = PacketResolver.type("net.minecraft.world.entity.EquipmentSlot");
        this.nmsEquipmentSlots = new Object[]{
                PacketResolver.staticFieldValue(equipmentSlotClass, "MAINHAND"),
                PacketResolver.staticFieldValue(equipmentSlotClass, "FEET"),
                PacketResolver.staticFieldValue(equipmentSlotClass, "LEGS"),
                PacketResolver.staticFieldValue(equipmentSlotClass, "CHEST"),
                PacketResolver.staticFieldValue(equipmentSlotClass, "HEAD"),
                PacketResolver.staticFieldValue(equipmentSlotClass, "OFFHAND")
        };

        // Teams & Scoreboard
        this.scoreboardClass = PacketResolver.type("net.minecraft.world.scores.Scoreboard");
        this.playerTeamClass = PacketResolver.type("net.minecraft.world.scores.PlayerTeam");
        this.scoreboardConstructor = PacketResolver.constructor(scoreboardClass);
        this.playerTeamConstructor = PacketResolver.constructor(playerTeamClass, scoreboardClass, String.class);

        Class<?> teamVisibilityClass = PacketResolver.type("net.minecraft.world.scores.Team$Visibility");
        this.teamSetVisibility = PacketResolver.method(playerTeamClass, "setNameTagVisibility", void.class, teamVisibilityClass);
        this.teamVisibilityNever = PacketResolver.staticFieldValue(teamVisibilityClass, "NEVER");

        Optional<Class<?>> collisionRuleClass = PacketResolver.optionalType("net.minecraft.world.scores.Team$CollisionRule");
        if (collisionRuleClass.isPresent()) {
            this.teamSetCollision = PacketResolver.optionalMethod(playerTeamClass, "setCollisionRule", void.class, collisionRuleClass.get()).orElse(null);
            this.teamCollisionNever = PacketResolver.optionalStaticFieldValue(collisionRuleClass.get(), "NEVER").orElse(null);
        } else {
            this.teamSetCollision = null;
            this.teamCollisionNever = null;
        }

        Optional<Class<?>> chatFormattingClass = PacketResolver.optionalType("net.minecraft.ChatFormatting");
        this.chatFormattingClass = chatFormattingClass.orElse(null);
        this.teamSetColor = chatFormattingClass.flatMap(c ->
                PacketResolver.optionalMethod(playerTeamClass, "setColor", void.class, c)
        ).orElse(null);

        this.scoreboardAddPlayerToTeam = PacketResolver.method(scoreboardClass, "addPlayerToTeam", boolean.class, String.class, playerTeamClass);

        Class<?> setPlayerTeamPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
        this.teamCreatePacket = PacketResolver.method(setPlayerTeamPacketClass, "createAddOrModifyPacket", setPlayerTeamPacketClass, playerTeamClass, boolean.class);
        this.teamRemovePacket = PacketResolver.method(setPlayerTeamPacketClass, "createRemovePacket", setPlayerTeamPacketClass, playerTeamClass);

        // Attributes
        Class<?> updateAttributesPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket");
        Class<?> attributeSnapshotClass = PacketResolver.type("net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket$AttributeSnapshot");
        Class<?> holderClass = PacketResolver.type("net.minecraft.core.Holder");
        this.attributeSnapshotConstructor = PacketResolver.constructor(attributeSnapshotClass, holderClass, double.class, Collection.class);
        this.updateAttributesConstructor = PacketResolver.constructor(updateAttributesPacketClass, int.class, List.class);

        Class<?> attributesClass = PacketResolver.type("net.minecraft.world.entity.ai.attributes.Attributes");
        this.scaleAttribute = PacketResolver.staticFieldValue(attributesClass, "SCALE");
        this.maxHealthAttribute = PacketResolver.optionalStaticFieldValue(attributesClass, "MAX_HEALTH").orElse(null);

        // Inbound Packets
        this.attackPacketClass = PacketResolver.optionalType("net.minecraft.network.protocol.game.ServerboundAttackPacket").orElse(null);
        this.interactPacketClass = PacketResolver.type("net.minecraft.network.protocol.game.ServerboundInteractPacket");

        this.attackEntityIdGetter = attackPacketClass != null
                ? PacketResolver.optionalMethod(attackPacketClass, "entityId", int.class)
                .or(() -> PacketResolver.optionalGetter(attackPacketClass, "entityId", "id")).orElse(null)
                : null;

        this.interactEntityIdGetter = PacketResolver.optionalMethod(interactPacketClass, "entityId", int.class)
                .or(() -> PacketResolver.optionalMethod(interactPacketClass, "getEntityId", int.class))
                .or(() -> PacketResolver.optionalGetter(interactPacketClass, "entityId", "id"))
                .orElseThrow(() -> new NoSuchMethodException("ServerboundInteractPacket.entityId"));

        this.interactIsAttackMethod = PacketResolver.optionalMethod(interactPacketClass, "isAttack", boolean.class).orElse(null);

        Optional<Class<?>> interactActionClass = PacketResolver.optionalType("net.minecraft.network.protocol.game.ServerboundInteractPacket$Action");
        this.interactActionGetter = interactActionClass.flatMap(c ->
                PacketResolver.optionalGetter(interactPacketClass, "action")
        ).orElse(null);

        this.interactActionTypeGetter = interactActionClass.flatMap(c ->
                PacketResolver.optionalMethod(c, "getType")
                        .or(() -> PacketResolver.optionalMethod(c, "type"))
        ).orElse(null);

        // Metadata indices are inherited and can move whenever Mojang inserts a
        // field in a base entity class. Resolve them from their accessors instead
        // of sending values at legacy fixed indices.
        int detectedSkinIndex = detectDataAccessorIndex(
                new String[]{"net.minecraft.world.entity.Avatar", "net.minecraft.world.entity.player.Player"},
                new String[]{"DATA_PLAYER_MODE_CUSTOMISATION", "DATA_PLAYER_MODE_CUSTOMIZATION"}
        );
        int detectedHealthIndex = detectDataAccessorIndex(
                new String[]{"net.minecraft.world.entity.LivingEntity"},
                new String[]{"DATA_HEALTH_ID"}
        );
        int detectedArmorStandFlagsIndex = detectDataAccessorIndex(
                new String[]{"net.minecraft.world.entity.decoration.ArmorStand"},
                new String[]{"DATA_CLIENT_FLAGS"}
        );

        this.capabilities = new ProtocolCapabilities(
                attackPacketClass != null,
                positionRotationConstructor != null,
                scaleAttribute != null,
                maxHealthAttribute != null,
                detectedSkinIndex,
                detectedHealthIndex,
                detectedArmorStandFlagsIndex
        );
    }

    private static int detectDataAccessorIndex(String[] classNames, String[] fieldNames) {
        for (String className : classNames) {
            try {
                Class<?> entityClass = Class.forName(className);
                for (String fieldName : fieldNames) {
                    try {
                        java.lang.reflect.Field field = entityClass.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object accessor = field.get(null);
                        java.lang.reflect.Method idMethod = accessor.getClass().getMethod("id");
                        return ((Number) idMethod.invoke(accessor)).intValue();
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            } catch (Throwable ignored) {
                // Try the next owner class. Missing optional metadata must be
                // omitted; guessing an index can disconnect the client.
            }
        }
        return -1;
    }
}
