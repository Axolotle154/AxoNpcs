package org.axostudio.axonpcs.packet;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.api.model.NPCSkinMode;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.ColorUtil;
import org.axostudio.axonpcs.util.PacketEventsGuard;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NPCPacketManager {
    private final AxoNPCsPlugin plugin;
    private final Map<UUID, Map<String, NPC>> sessions = new ConcurrentHashMap<>();

    public NPCPacketManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean show(Player player, VirtualNPC npc) {
        if (!PacketEventsGuard.canUsePacketEvents(plugin)) {
            return false;
        }
        if (!npc.getType().equalsIgnoreCase("PLAYER")) {
            plugin.getLogger().fine("Only PLAYER packet NPCs are implemented right now; got " + npc.getType());
        }
        hide(player, npc);
        NPC packetNpc = createPacketNPC(player, npc);
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        sendTeam(player, npc, packetNpc.getProfile().getName());
        packetNpc.spawn(channel);
        packetNpc.updateRotation(npc.getPosition().yaw(), npc.getPosition().pitch());
        applyMetadata(player, npc);
        applyScale(player, npc);
        sessions.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(npc.getId(), packetNpc);
        return true;
    }

    public void hide(Player player, VirtualNPC npc) {
        Map<String, NPC> playerSessions = sessions.get(player.getUniqueId());
        if (playerSessions == null) {
            return;
        }
        NPC packetNpc = playerSessions.remove(npc.getId());
        if (packetNpc == null) {
            return;
        }
        if (!PacketEventsGuard.canUsePacketEvents(plugin)) {
            return;
        }
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        packetNpc.despawn(channel);
        removeTeam(player, npc);
        if (playerSessions.isEmpty()) {
            sessions.remove(player.getUniqueId());
        }
    }

    public void hideAll(Player player) {
        Map<String, NPC> playerSessions = sessions.remove(player.getUniqueId());
        if (playerSessions == null) {
            return;
        }
        if (!PacketEventsGuard.canUsePacketEvents(plugin)) {
            return;
        }
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        for (Map.Entry<String, NPC> entry : playerSessions.entrySet()) {
            entry.getValue().despawn(channel);
            plugin.getNpcManager().get(entry.getKey()).ifPresent(npc -> removeTeam(player, npc));
        }
    }

    public void updateRotation(VirtualNPC npc) {
        for (Map<String, NPC> playerSessions : sessions.values()) {
            NPC packetNpc = playerSessions.get(npc.getId());
            if (packetNpc != null) {
                packetNpc.updateRotation(npc.getPosition().yaw(), npc.getPosition().pitch());
            }
        }
    }

    public int viewerCount(VirtualNPC npc) {
        int count = 0;
        for (Map<String, NPC> playerSessions : sessions.values()) {
            if (playerSessions.containsKey(npc.getId())) {
                count++;
            }
        }
        return count;
    }

    private NPC createPacketNPC(Player viewer, VirtualNPC npc) {
        Component nameComponent = npc.getDisplayName() == null ? Component.text(npc.getId()) : ColorUtil.parse(npc.getDisplayName());
        List<TextureProperty> textures = texturesFor(viewer, npc.getSkin());
        UserProfile profile = new UserProfile(npc.getUniqueId(), profileName(npc), textures);
        NPC packetNpc = new NPC(profile, npc.getEntityId(), GameMode.CREATIVE, nameComponent, null, null, null);
        packetNpc.setLocation(SpigotConversionUtil.fromBukkitLocation(npc.getLocation()));
        applyEquipment(packetNpc, npc);
        return packetNpc;
    }

    private void applyEquipment(NPC packetNpc, VirtualNPC npc) {
        setEquipment(packetNpc, NPCEquipmentSlot.HELMET, npc.getEquipment().get(NPCEquipmentSlot.HELMET));
        setEquipment(packetNpc, NPCEquipmentSlot.CHESTPLATE, npc.getEquipment().get(NPCEquipmentSlot.CHESTPLATE));
        setEquipment(packetNpc, NPCEquipmentSlot.LEGGINGS, npc.getEquipment().get(NPCEquipmentSlot.LEGGINGS));
        setEquipment(packetNpc, NPCEquipmentSlot.BOOTS, npc.getEquipment().get(NPCEquipmentSlot.BOOTS));
        setEquipment(packetNpc, NPCEquipmentSlot.MAIN_HAND, npc.getEquipment().get(NPCEquipmentSlot.MAIN_HAND));
        setEquipment(packetNpc, NPCEquipmentSlot.OFF_HAND, npc.getEquipment().get(NPCEquipmentSlot.OFF_HAND));
    }

    private void setEquipment(NPC packetNpc, NPCEquipmentSlot slot, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        com.github.retrooper.packetevents.protocol.item.ItemStack packetItem = SpigotConversionUtil.fromBukkitItemStack(itemStack);
        switch (slot) {
            case HELMET:
                packetNpc.setHelmet(packetItem);
                break;
            case CHESTPLATE:
                packetNpc.setChestplate(packetItem);
                break;
            case LEGGINGS:
                packetNpc.setLeggings(packetItem);
                break;
            case BOOTS:
                packetNpc.setBoots(packetItem);
                break;
            case MAIN_HAND:
                packetNpc.setMainHand(packetItem);
                break;
            case OFF_HAND:
                packetNpc.setOffHand(packetItem);
                break;
            default:
                break;
        }
    }

    private List<TextureProperty> texturesFor(Player viewer, NPCSkin skin) {
        if (skin.mode() == NPCSkinMode.MIRROR) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
            if (user != null && user.getProfile() != null) {
                return new ArrayList<>(user.getProfile().getTextureProperties());
            }
            return List.of();
        }
        if (skin.mode() == NPCSkinMode.TEXTURE && !skin.value().isBlank()) {
            return List.of(new TextureProperty("textures", skin.value(), skin.signature().isBlank() ? null : skin.signature()));
        }
        return List.of();
    }

    private void applyMetadata(Player player, VirtualNPC npc) {
        byte flags = 0;
        if (!npc.getGlowing().equalsIgnoreCase("off")) {
            flags |= 0x40;
        }
        Collection<EntityData<?>> metadata = List.of(new EntityData<>(0, EntityDataTypes.BYTE, flags));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(npc.getEntityId(), new ArrayList<>(metadata)));
    }

    private void applyScale(Player player, VirtualNPC npc) {
        if (Math.abs(npc.getScale() - 1.0D) < 0.0001D) {
            return;
        }
        WrapperPlayServerUpdateAttributes.Property property =
                new WrapperPlayServerUpdateAttributes.Property("minecraft:scale", npc.getScale(), List.of());
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerUpdateAttributes(npc.getEntityId(), List.of(property)));
    }

    private void sendTeam(Player player, VirtualNPC npc, String entityName) {
        NamedTextColor color = npc.getGlowing().equalsIgnoreCase("off")
                ? NamedTextColor.WHITE
                : ColorUtil.namedColor(npc.getGlowing(), NamedTextColor.WHITE);
        WrapperPlayServerTeams.CollisionRule collisionRule = npc.isCollidable()
                ? WrapperPlayServerTeams.CollisionRule.ALWAYS
                : WrapperPlayServerTeams.CollisionRule.NEVER;
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.text(teamName(npc)),
                Component.empty(),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                collisionRule,
                color,
                WrapperPlayServerTeams.OptionData.NONE
        );
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                teamName(npc),
                WrapperPlayServerTeams.TeamMode.CREATE,
                info,
                List.of(entityName)
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private void removeTeam(Player player, VirtualNPC npc) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                teamName(npc),
                WrapperPlayServerTeams.TeamMode.REMOVE,
                (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                List.of()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
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
}
