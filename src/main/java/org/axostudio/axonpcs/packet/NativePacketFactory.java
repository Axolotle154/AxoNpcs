package org.axostudio.axonpcs.packet;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.api.model.NPCSkinMode;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class NativePacketFactory {
    private final AxoNPCsPlugin plugin;

    private final Class<?> packetClass = type("net.minecraft.network.protocol.Packet");
    private final Class<?> craftPlayerClass = type("org.bukkit.craftbukkit.entity.CraftPlayer");
    private final Method craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle");
    private final Field serverPlayerConnectionField = type("net.minecraft.server.level.ServerPlayer").getField("connection");
    private final Field connectionField = type("net.minecraft.server.network.ServerCommonPacketListenerImpl").getField("connection");
    private final Field channelField = type("net.minecraft.network.Connection").getField("channel");
    private final Method sendMethod = type("net.minecraft.server.network.ServerCommonPacketListenerImpl").getMethod("send", packetClass);

    private final Class<?> gameProfileClass = type("com.mojang.authlib.GameProfile");
    private final Class<?> propertyClass = type("com.mojang.authlib.properties.Property");
    private final Class<?> propertyMapClass = type("com.mojang.authlib.properties.PropertyMap");
    private final Class<?> multimapClass = type("com.google.common.collect.Multimap");
    private final Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class, propertyMapClass);
    private final Constructor<?> propertyMapConstructor = propertyMapClass.getConstructor(multimapClass);
    private final Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class, String.class);
    private final Method arrayListMultimapCreate = type("com.google.common.collect.ArrayListMultimap").getMethod("create");
    private final Method gameProfileProperties = gameProfileClass.getMethod("properties");
    private final Method gameProfileName = gameProfileClass.getMethod("name");
    private final Method propertyName = propertyClass.getMethod("name");
    private final Method propertyValue = propertyClass.getMethod("value");
    private final Method propertySignature = propertyClass.getMethod("signature");

    private final Class<?> nmsComponentClass = type("net.minecraft.network.chat.Component");
    private final Method literalComponent = nmsComponentClass.getMethod("literal", String.class);
    private final Method paperAdventureAsVanilla = type("io.papermc.paper.adventure.PaperAdventure").getMethod("asVanilla", Component.class);

    private final Class<?> playerInfoPacketClass = type("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
    private final Class<?> playerInfoEntryClass = type("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
    private final Class<?> playerInfoActionClass = type("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
    private final Class<?> gameTypeClass = type("net.minecraft.world.level.GameType");
    private final Class<?> chatSessionDataClass = type("net.minecraft.network.chat.RemoteChatSession$Data");
    private final Constructor<?> playerInfoEntryConstructor = playerInfoEntryClass.getConstructor(
            UUID.class, gameProfileClass, boolean.class, int.class, gameTypeClass, nmsComponentClass, boolean.class, int.class, chatSessionDataClass
    );
    private final Constructor<?> playerInfoPacketConstructor = playerInfoPacketClass.getConstructor(EnumSet.class, List.class);
    private final Constructor<?> playerInfoRemoveConstructor = type("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket").getConstructor(List.class);

    private final Class<?> entityTypeClass = type("net.minecraft.world.entity.EntityType");
    private final Class<?> vec3Class = type("net.minecraft.world.phys.Vec3");
    private final Constructor<?> vec3Constructor = vec3Class.getConstructor(double.class, double.class, double.class);
    private final Constructor<?> addEntityConstructor = type("net.minecraft.network.protocol.game.ClientboundAddEntityPacket").getConstructor(
            int.class, UUID.class, double.class, double.class, double.class, float.class, float.class,
            entityTypeClass, int.class, vec3Class, double.class
    );
    private final Constructor<?> removeEntityConstructor = type("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket").getConstructor(int[].class);
    private final Constructor<?> bodyRotationConstructor = type("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Rot")
            .getConstructor(int.class, byte.class, byte.class, boolean.class);
    private final Class<?> headRotationPacketClass = type("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
    private final Object unsafe = unsafe();
    private final Method unsafeAllocateInstance = unsafe == null ? null : unsafe.getClass().getMethod("allocateInstance", Class.class);
    private final Method unsafePutInt = unsafe == null ? null : unsafe.getClass().getMethod("putInt", Object.class, long.class, int.class);
    private final Method unsafePutByte = unsafe == null ? null : unsafe.getClass().getMethod("putByte", Object.class, long.class, byte.class);
    private final Method unsafeObjectFieldOffset = unsafe == null ? null : unsafe.getClass().getMethod("objectFieldOffset", Field.class);
    private final Field headRotationEntityIdField = headRotationPacketClass.getDeclaredField("entityId");
    private final Field headRotationValueField = headRotationPacketClass.getDeclaredField("yHeadRot");

    private final Class<?> dataValueClass = type("net.minecraft.network.syncher.SynchedEntityData$DataValue");
    private final Class<?> dataSerializerClass = type("net.minecraft.network.syncher.EntityDataSerializer");
    private final Constructor<?> dataValueConstructor = dataValueClass.getConstructor(int.class, dataSerializerClass, Object.class);
    private final Constructor<?> metadataConstructor = type("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket")
            .getConstructor(int.class, List.class);
    private final Object byteSerializer = staticField("net.minecraft.network.syncher.EntityDataSerializers", "BYTE");
    private final Object booleanSerializer = staticField("net.minecraft.network.syncher.EntityDataSerializers", "BOOLEAN");
    private final Object optionalComponentSerializer = staticField("net.minecraft.network.syncher.EntityDataSerializers", "OPTIONAL_COMPONENT");

    private final Class<?> attributeSnapshotClass = type("net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket$AttributeSnapshot");
    private final Constructor<?> attributeSnapshotConstructor = attributeSnapshotClass.getConstructor(
            type("net.minecraft.core.Holder"), double.class, Collection.class
    );
    private final Constructor<?> updateAttributesConstructor = privateConstructor(
            "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket", int.class, List.class
    );
    private final Object scaleAttribute = staticField("net.minecraft.world.entity.ai.attributes.Attributes", "SCALE");

    private final Class<?> equipmentSlotClass = type("net.minecraft.world.entity.EquipmentSlot");
    private final Class<?> nmsItemStackClass = type("net.minecraft.world.item.ItemStack");
    private final Constructor<?> equipmentPacketConstructor = type("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket")
            .getConstructor(int.class, List.class);
    private final Method craftItemAsNmsCopy = type("org.bukkit.craftbukkit.inventory.CraftItemStack")
            .getMethod("asNMSCopy", ItemStack.class);
    private final Class<?> pairClass = type("com.mojang.datafixers.util.Pair");
    private final Method pairOf = pairClass.getMethod("of", Object.class, Object.class);

    private final Class<?> scoreboardClass = type("net.minecraft.world.scores.Scoreboard");
    private final Class<?> playerTeamClass = type("net.minecraft.world.scores.PlayerTeam");
    private final Constructor<?> scoreboardConstructor = scoreboardClass.getConstructor();
    private final Constructor<?> playerTeamConstructor = playerTeamClass.getConstructor(scoreboardClass, String.class);
    private final Method teamSetDisplayName = playerTeamClass.getMethod("setDisplayName", nmsComponentClass);
    private final Method teamSetPrefix = playerTeamClass.getMethod("setPlayerPrefix", nmsComponentClass);
    private final Method teamSetSuffix = playerTeamClass.getMethod("setPlayerSuffix", nmsComponentClass);
    private final Method teamSetVisibility = playerTeamClass.getMethod("setNameTagVisibility", type("net.minecraft.world.scores.Team$Visibility"));
    private final Method teamSetCollision = playerTeamClass.getMethod("setCollisionRule", type("net.minecraft.world.scores.Team$CollisionRule"));
    private final Method teamSetColor = playerTeamClass.getMethod("setColor", type("net.minecraft.ChatFormatting"));
    private final Method scoreboardAddPlayerToTeam = scoreboardClass.getMethod("addPlayerToTeam", String.class, playerTeamClass);
    private final Method teamCreatePacket = type("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket")
            .getMethod("createAddOrModifyPacket", playerTeamClass, boolean.class);
    private final Method teamRemovePacket = type("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket")
            .getMethod("createRemovePacket", playerTeamClass);

    private final Class<?> attackPacketClass = optionalType("net.minecraft.network.protocol.game.ServerboundAttackPacket");
    private final Class<?> interactPacketClass = type("net.minecraft.network.protocol.game.ServerboundInteractPacket");
    private final Method attackEntityId = optionalMethod(attackPacketClass, "entityId");
    private final Field attackEntityIdField = firstField(attackPacketClass, int.class, "entityId", "id");
    private final Method interactEntityId = firstMethod(interactPacketClass, "entityId", "getEntityId");
    private final Field interactEntityIdField = firstField(interactPacketClass, int.class, "entityId", "id");
    private final Method interactIsAttack = optionalMethod(interactPacketClass, "isAttack");
    private final Class<?> interactActionClass = optionalType("net.minecraft.network.protocol.game.ServerboundInteractPacket$Action");
    private final Field interactActionField = firstField(interactPacketClass, interactActionClass, "action");
    private final Method interactActionGetType = firstMethod(interactActionClass, "getType", "type");

    NativePacketFactory(AxoNPCsPlugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;
        headRotationEntityIdField.setAccessible(true);
        headRotationValueField.setAccessible(true);
    }

    Object channel(Player player) {
        try {
            Object handle = craftPlayerGetHandle.invoke(player);
            Object listener = serverPlayerConnectionField.get(handle);
            Object connection = connectionField.get(listener);
            return channelField.get(connection);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    void send(Player player, Object packet) throws ReflectiveOperationException {
        if (packet == null) {
            return;
        }
        Object handle = craftPlayerGetHandle.invoke(player);
        Object listener = serverPlayerConnectionField.get(handle);
        sendMethod.invoke(listener, packet);
    }

    NativePacketBackend.NativeNpcSession createSession(Player viewer, VirtualNPC npc) throws ReflectiveOperationException {
        String profileName = profileName(npc);
        Object profile = createGameProfile(npc.getUniqueId(), profileName, textureProperties(viewer, npc.getSkin()));
        return new NativePacketBackend.NativeNpcSession(npc, profileName, teamName(npc), profile);
    }

    Object playerInfoAdd(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        Object displayName = session.npc().getDisplayName() == null ? null : vanillaComponent(session.npc().getDisplayName());
        Object entry = playerInfoEntryConstructor.newInstance(
                session.npc().getUniqueId(),
                session.gameProfile(),
                false,
                0,
                staticField(gameTypeClass, "CREATIVE"),
                displayName,
                true,
                0,
                null
        );
        @SuppressWarnings({"rawtypes", "unchecked"})
        EnumSet actions = EnumSet.of(
                (Enum) staticField(playerInfoActionClass, "ADD_PLAYER"),
                (Enum) staticField(playerInfoActionClass, "UPDATE_GAME_MODE"),
                (Enum) staticField(playerInfoActionClass, "UPDATE_LISTED"),
                (Enum) staticField(playerInfoActionClass, "UPDATE_LATENCY"),
                (Enum) staticField(playerInfoActionClass, "UPDATE_DISPLAY_NAME"),
                (Enum) staticField(playerInfoActionClass, "UPDATE_HAT")
        );
        return playerInfoPacketConstructor.newInstance(actions, List.of(entry));
    }

    Object playerInfoRemove(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        return playerInfoRemoveConstructor.newInstance(List.of(session.npc().getUniqueId()));
    }

    Object addEntity(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        VirtualNPC npc = session.npc();
        return addEntityConstructor.newInstance(
                npc.getEntityId(),
                npc.getUniqueId(),
                npc.getPosition().x(),
                npc.getPosition().y(),
                npc.getPosition().z(),
                npc.getPosition().pitch(),
                npc.getPosition().yaw(),
                staticField(entityTypeClass, "PLAYER"),
                0,
                vec3Constructor.newInstance(0.0D, 0.0D, 0.0D),
                (double) npc.getPosition().yaw()
        );
    }

    Object removeEntity(VirtualNPC npc) throws ReflectiveOperationException {
        return removeEntityConstructor.newInstance((Object) new int[]{npc.getEntityId()});
    }

    Object metadata(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        VirtualNPC npc = session.npc();
        List<Object> metadata = new ArrayList<>();
        byte flags = 0;
        if (!npc.getGlowing().equalsIgnoreCase("off")) {
            flags |= 0x40;
        }
        metadata.add(dataValueConstructor.newInstance(0, byteSerializer, flags));
        if (npc.getDisplayName() != null) {
            metadata.add(dataValueConstructor.newInstance(2, optionalComponentSerializer, Optional.of(vanillaComponent(npc.getDisplayName()))));
            metadata.add(dataValueConstructor.newInstance(3, booleanSerializer, true));
        }
        return metadataConstructor.newInstance(npc.getEntityId(), metadata);
    }

    Object attributes(VirtualNPC npc) throws ReflectiveOperationException {
        if (Math.abs(npc.getScale() - 1.0D) < 0.0001D) {
            return null;
        }
        Object snapshot = attributeSnapshotConstructor.newInstance(scaleAttribute, npc.getScale(), List.of());
        return updateAttributesConstructor.newInstance(npc.getEntityId(), List.of(snapshot));
    }

    Object equipment(VirtualNPC npc) throws ReflectiveOperationException {
        List<Object> pairs = new ArrayList<>();
        for (Map.Entry<NPCEquipmentSlot, ItemStack> entry : npc.getEquipment().entrySet()) {
            ItemStack itemStack = entry.getValue();
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            Object nmsItem = craftItemAsNmsCopy.invoke(null, itemStack);
            pairs.add(pairOf.invoke(null, equipmentSlot(entry.getKey()), nmsItem));
        }
        if (pairs.isEmpty()) {
            return null;
        }
        return equipmentPacketConstructor.newInstance(npc.getEntityId(), pairs);
    }

    Object bodyRotation(VirtualNPC npc) throws ReflectiveOperationException {
        return bodyRotation(npc, npc.getPosition().yaw(), npc.getPosition().pitch());
    }

    Object bodyRotation(VirtualNPC npc, float yaw, float pitch) throws ReflectiveOperationException {
        return bodyRotationConstructor.newInstance(npc.getEntityId(), packedAngle(yaw), packedAngle(pitch), true);
    }

    Object headRotation(VirtualNPC npc) throws ReflectiveOperationException {
        return headRotation(npc, npc.getPosition().yaw());
    }

    Object headRotation(VirtualNPC npc, float yaw) throws ReflectiveOperationException {
        if (unsafe == null) {
            return null;
        }
        Object packet = unsafeAllocateInstance.invoke(unsafe, headRotationPacketClass);
        long entityIdOffset = (Long) unsafeObjectFieldOffset.invoke(unsafe, headRotationEntityIdField);
        long headRotOffset = (Long) unsafeObjectFieldOffset.invoke(unsafe, headRotationValueField);
        unsafePutInt.invoke(unsafe, packet, entityIdOffset, npc.getEntityId());
        unsafePutByte.invoke(unsafe, packet, headRotOffset, packedAngle(yaw));
        return packet;
    }

    Object teamCreate(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        return teamCreatePacket.invoke(null, team(session), true);
    }

    Object teamRemove(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        return teamRemovePacket.invoke(null, team(session));
    }

    NPCActionTrigger interactionTrigger(Object packet) {
        if (attackPacketClass != null && attackPacketClass.isInstance(packet)) {
            return NPCActionTrigger.LEFT_CLICK;
        }
        if (interactPacketClass.isInstance(packet)) {
            return interactTrigger(packet);
        }
        return null;
    }

    int interactionEntityId(Object packet) {
        try {
            if (attackPacketClass != null && attackPacketClass.isInstance(packet)) {
                return entityId(packet, attackEntityId, attackEntityIdField);
            }
            if (interactPacketClass.isInstance(packet)) {
                return entityId(packet, interactEntityId, interactEntityIdField);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return -1;
    }

    private NPCActionTrigger interactTrigger(Object packet) {
        try {
            if (interactIsAttack != null) {
                return Boolean.TRUE.equals(interactIsAttack.invoke(packet)) ? NPCActionTrigger.LEFT_CLICK : NPCActionTrigger.RIGHT_CLICK;
            }
            if (interactActionField == null) {
                return NPCActionTrigger.RIGHT_CLICK;
            }
            Object action = interactActionField.get(packet);
            if (action == null) {
                return NPCActionTrigger.RIGHT_CLICK;
            }
            Object type = actionType(action);
            String typeName = enumName(type);
            if ("ATTACK".equals(typeName)) {
                return NPCActionTrigger.LEFT_CLICK;
            }
            String actionName = action.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            return actionName.contains("ATTACK") ? NPCActionTrigger.LEFT_CLICK : NPCActionTrigger.RIGHT_CLICK;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return NPCActionTrigger.RIGHT_CLICK;
        }
    }

    private int entityId(Object packet, Method method, Field field) throws ReflectiveOperationException {
        Object value;
        if (method != null) {
            value = method.invoke(packet);
        } else if (field != null) {
            value = field.get(packet);
        } else {
            return -1;
        }
        return value instanceof Number number ? number.intValue() : -1;
    }

    private Object actionType(Object action) throws ReflectiveOperationException {
        if (interactActionGetType != null) {
            return interactActionGetType.invoke(action);
        }
        Field field = firstEnumField(action.getClass(), "type");
        return field == null ? null : field.get(action);
    }

    private String enumName(Object value) {
        return value instanceof Enum<?> enumValue ? enumValue.name() : "";
    }

    private Object team(NativePacketBackend.NativeNpcSession session) throws ReflectiveOperationException {
        VirtualNPC npc = session.npc();
        Object scoreboard = scoreboardConstructor.newInstance();
        Object team = playerTeamConstructor.newInstance(scoreboard, session.teamName());
        teamSetDisplayName.invoke(team, literal(""));
        teamSetPrefix.invoke(team, literal(""));
        teamSetSuffix.invoke(team, literal(""));
        teamSetVisibility.invoke(team, enumValue("net.minecraft.world.scores.Team$Visibility", npc.getDisplayName() == null ? "NEVER" : "ALWAYS"));
        teamSetCollision.invoke(team, enumValue("net.minecraft.world.scores.Team$CollisionRule", npc.isCollidable() ? "ALWAYS" : "NEVER"));
        teamSetColor.invoke(team, chatColor(npc.getGlowing()));
        scoreboardAddPlayerToTeam.invoke(scoreboard, session.profileName(), team);
        return team;
    }

    private Object equipmentSlot(NPCEquipmentSlot slot) throws ReflectiveOperationException {
        String nmsName = switch (slot) {
            case HELMET -> "HEAD";
            case CHESTPLATE -> "CHEST";
            case LEGGINGS -> "LEGS";
            case BOOTS -> "FEET";
            case MAIN_HAND -> "MAINHAND";
            case OFF_HAND -> "OFFHAND";
        };
        return staticField(equipmentSlotClass, nmsName);
    }

    private Object createGameProfile(UUID uuid, String name, List<TextureProperty> textures) throws ReflectiveOperationException {
        Object multimap = arrayListMultimapCreate.invoke(null);
        Method put = multimap.getClass().getMethod("put", Object.class, Object.class);
        for (TextureProperty texture : textures) {
            Object property = propertyConstructor.newInstance(
                    "textures",
                    texture.value(),
                    texture.signature() == null || texture.signature().isBlank() ? null : texture.signature()
            );
            put.invoke(multimap, "textures", property);
        }
        Object propertyMap = propertyMapConstructor.newInstance(multimap);
        return gameProfileConstructor.newInstance(uuid, name, propertyMap);
    }

    private List<TextureProperty> textureProperties(Player viewer, NPCSkin skin) throws ReflectiveOperationException {
        if (skin.mode() == NPCSkinMode.MIRROR) {
            return copyTextureProperties(viewerProfile(viewer));
        }
        if (skin.mode() == NPCSkinMode.TEXTURE && !skin.value().isBlank()) {
            return List.of(new TextureProperty(skin.value(), skin.signature()));
        }
        return List.of();
    }

    private Object viewerProfile(Player viewer) throws ReflectiveOperationException {
        Object craftPlayer = craftPlayerClass.cast(viewer);
        Method getProfile = craftPlayerClass.getMethod("getProfile");
        return getProfile.invoke(craftPlayer);
    }

    private List<TextureProperty> copyTextureProperties(Object sourceProfile) throws ReflectiveOperationException {
        Object sourceProperties = gameProfileProperties.invoke(sourceProfile);
        Method get = sourceProperties.getClass().getMethod("get", Object.class);
        Collection<?> textures = (Collection<?>) get.invoke(sourceProperties, "textures");
        List<TextureProperty> result = new ArrayList<>();
        for (Object texture : textures) {
            String value = (String) propertyValue.invoke(texture);
            String signature = (String) propertySignature.invoke(texture);
            result.add(new TextureProperty(value, signature));
        }
        return result;
    }

    private Object vanillaComponent(String input) {
        try {
            return paperAdventureAsVanilla.invoke(null, ColorUtil.parse(input));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return literal(ColorUtil.plain(input));
        }
    }

    private Object literal(String text) {
        try {
            return literalComponent.invoke(null, text);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create vanilla component", exception);
        }
    }

    private Object chatColor(String input) throws ReflectiveOperationException {
        if (input == null || input.equalsIgnoreCase("off")) {
            return staticField("net.minecraft.ChatFormatting", "WHITE");
        }
        String normalized = input.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DARKBLUE" -> staticField("net.minecraft.ChatFormatting", "DARK_BLUE");
            case "DARKGREEN" -> staticField("net.minecraft.ChatFormatting", "DARK_GREEN");
            case "DARKAQUA" -> staticField("net.minecraft.ChatFormatting", "DARK_AQUA");
            case "DARKRED" -> staticField("net.minecraft.ChatFormatting", "DARK_RED");
            case "DARKPURPLE" -> staticField("net.minecraft.ChatFormatting", "DARK_PURPLE");
            case "DARKGRAY", "DARKGREY" -> staticField("net.minecraft.ChatFormatting", "DARK_GRAY");
            case "LIGHTPURPLE", "PINK" -> staticField("net.minecraft.ChatFormatting", "LIGHT_PURPLE");
            default -> {
                try {
                    yield staticField("net.minecraft.ChatFormatting", normalized);
                } catch (ReflectiveOperationException exception) {
                    yield staticField("net.minecraft.ChatFormatting", "WHITE");
                }
            }
        };
    }

    private String profileName(VirtualNPC npc) {
        String base = npc.getDisplayName() == null ? npc.getId() : ColorUtil.plain(npc.getDisplayName());
        base = base.replaceAll("[^A-Za-z0-9_]", "_");
        if (base.length() < 3) {
            base = npc.getId() + "_npc";
        }
        if (base.length() > 16) {
            base = base.substring(0, 16);
        }
        return base;
    }

    private String teamName(VirtualNPC npc) {
        String hash = Integer.toHexString(npc.getId().hashCode()).replace("-", "");
        return ("axo" + hash).substring(0, Math.min(16, ("axo" + hash).length())).toLowerCase(Locale.ROOT);
    }

    private byte packedAngle(float angle) {
        return (byte) Math.floor(angle * 256.0F / 360.0F);
    }

    private static Constructor<?> privateConstructor(String className, Class<?>... parameters) throws ReflectiveOperationException {
        Constructor<?> constructor = type(className).getDeclaredConstructor(parameters);
        constructor.setAccessible(true);
        return constructor;
    }

    private static Class<?> optionalType(String name) {
        try {
            return type(name);
        } catch (ClassNotFoundException | LinkageError exception) {
            return null;
        }
    }

    private static Method firstMethod(Class<?> type, String... names) {
        for (String name : names) {
            Method method = optionalMethod(type, name);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private static Method optionalMethod(Class<?> type, String name, Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        try {
            Method method = type.getMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | SecurityException exception) {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                try {
                    Method method = current.getDeclaredMethod(name, parameters);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException | SecurityException ignored) {
                    current = current.getSuperclass();
                }
            }
            return null;
        }
    }

    private static Field optionalField(Class<?> type, String name) {
        if (type == null) {
            return null;
        }
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException exception) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Field firstField(Class<?> type, Class<?> fieldType, String... preferredNames) {
        if (type == null) {
            return null;
        }
        for (String name : preferredNames) {
            Field field = optionalField(type, name);
            if (field != null && fieldMatches(field, fieldType)) {
                return field;
            }
        }
        if (fieldType == null) {
            return null;
        }
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!fieldMatches(field, fieldType)) {
                    continue;
                }
                field.setAccessible(true);
                return field;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field firstEnumField(Class<?> type, String... preferredNames) {
        if (type == null) {
            return null;
        }
        for (String name : preferredNames) {
            Field field = optionalField(type, name);
            if (field != null && field.getType().isEnum()) {
                return field;
            }
        }
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.getType().isEnum()) {
                    continue;
                }
                field.setAccessible(true);
                return field;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean fieldMatches(Field field, Class<?> fieldType) {
        if (fieldType == null) {
            return true;
        }
        if (fieldType == int.class) {
            return field.getType() == int.class || field.getType() == Integer.class;
        }
        return fieldType.isAssignableFrom(field.getType());
    }

    private static Object staticField(String className, String fieldName) throws ReflectiveOperationException {
        return staticField(type(className), fieldName);
    }

    private static Object staticField(Class<?> type, String fieldName) throws ReflectiveOperationException {
        Field field = type.getField(fieldName);
        return field.get(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(String className, String value) throws ReflectiveOperationException {
        return Enum.valueOf((Class<? extends Enum>) type(className).asSubclass(Enum.class), value);
    }

    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, NativePacketFactory.class.getClassLoader());
    }

    private static Object unsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private record TextureProperty(String value, String signature) {
    }
}
