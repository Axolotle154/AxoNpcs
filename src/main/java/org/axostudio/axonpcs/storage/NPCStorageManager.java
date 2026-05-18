package org.axostudio.axonpcs.storage;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.api.model.NPCSkinMode;
import org.axostudio.axonpcs.model.NPCPosition;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.IdValidator;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class NPCStorageManager {
    private final AxoNPCsPlugin plugin;
    private final File directory;

    public NPCStorageManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "npcs");
    }

    public void init() {
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Could not create NPC directory");
        }
        plugin.saveResource("npcs/ejemplo.yml", false);
        plugin.saveResource("npcs/npc_1.yml", false);
        plugin.saveResource("npcs/npc_2.yml", false);
    }

    public List<VirtualNPC> loadAll() {
        init();
        List<VirtualNPC> loaded = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return loaded;
        }
        for (File file : files) {
            try {
                VirtualNPC npc = load(file);
                if (npc != null && npc.isEnabled()) {
                    loaded.add(npc);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Could not load NPC file " + file.getName() + ": " + exception.getMessage());
            }
        }
        return loaded;
    }

    public VirtualNPC load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String rawId = yaml.getString("id", file.getName().replace(".yml", ""));
        String id = IdValidator.requireValid(rawId);
        if (!file.getName().equals(id + ".yml")) {
            throw new IllegalArgumentException("File name must match safe NPC id: " + id + ".yml");
        }

        String world = yaml.getString("world", "world");
        ConfigurationSection position = yaml.getConfigurationSection("position");
        double x = position == null ? 0.0D : position.getDouble("x");
        double y = position == null ? 64.0D : position.getDouble("y");
        double z = position == null ? 0.0D : position.getDouble("z");
        float yaw = (float) (position == null ? 0.0D : position.getDouble("yaw"));
        float pitch = (float) (position == null ? 0.0D : position.getDouble("pitch"));

        UUID uuid = parseUuid(yaml.getString("uuid"), id);
        int entityId = yaml.getInt("entity-id", SpigotReflectionUtil.generateEntityId());
        VirtualNPC npc = new VirtualNPC(id, uuid, entityId, new NPCPosition(world, x, y, z, yaw, pitch));
        npc.setEnabled(yaml.getBoolean("enabled", true));
        npc.setType(yaml.getString("type", "PLAYER"));
        npc.setDisplayName(yaml.getString("display-name", null));
        npc.setSkin(readSkin(yaml.getConfigurationSection("skin")));
        npc.setGlowing(yaml.getString("glowing", "off"));
        npc.setCollidable(yaml.getBoolean("collidable", false));
        npc.setScale(yaml.getDouble("scale", 1.0D));
        npc.setViewDistance(yaml.getDouble("view-distance", plugin.getConfig().getDouble("rendering.view-distance", 48.0D)));
        npc.setInteractionCooldownSeconds(readCooldown(yaml.get("interaction-cooldown")));
        readEquipment(npc, yaml.getConfigurationSection("equipment"));
        readActions(npc, yaml.getConfigurationSection("actions"));
        return npc;
    }

    public void save(VirtualNPC npc) {
        init();
        File file = fileFor(npc.getId());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("enabled", npc.isEnabled());
        yaml.set("id", npc.getId());
        yaml.set("uuid", npc.getUniqueId().toString());
        yaml.set("entity-id", npc.getEntityId());
        yaml.set("type", npc.getType());
        yaml.set("world", npc.getPosition().world());
        yaml.set("position.x", npc.getPosition().x());
        yaml.set("position.y", npc.getPosition().y());
        yaml.set("position.z", npc.getPosition().z());
        yaml.set("position.yaw", npc.getPosition().yaw());
        yaml.set("position.pitch", npc.getPosition().pitch());
        yaml.set("display-name", npc.getDisplayName());
        yaml.set("skin.mode", npc.getSkin().mode().name());
        yaml.set("skin.source", npc.getSkin().source());
        yaml.set("skin.value", npc.getSkin().value());
        yaml.set("skin.signature", npc.getSkin().signature());
        yaml.set("glowing", npc.getGlowing());
        yaml.set("collidable", npc.isCollidable());
        yaml.set("scale", npc.getScale());
        yaml.set("view-distance", npc.getViewDistance());
        yaml.set("interaction-cooldown", npc.getInteractionCooldownSeconds() <= 0 ? "disabled" : npc.getInteractionCooldownSeconds());
        for (var entry : npc.getEquipment().entrySet()) {
            yaml.set("equipment." + entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue());
        }
        for (var entry : npc.getActionsByTrigger().entrySet()) {
            int index = 0;
            for (NPCAction action : entry.getValue()) {
                String path = "actions." + entry.getKey().name() + "." + index++;
                yaml.set(path + ".type", action.type());
                yaml.set(path + ".value", action.value());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save NPC " + npc.getId(), exception);
        }
    }

    public boolean delete(String id) {
        File file = fileFor(id);
        return !file.exists() || file.delete();
    }

    public File fileFor(String id) {
        String safeId = IdValidator.requireValid(id);
        File file = new File(directory, safeId + ".yml");
        File absoluteDirectory = directory.getAbsoluteFile();
        File absoluteFile = file.getAbsoluteFile();
        if (!absoluteFile.getParentFile().equals(absoluteDirectory)) {
            throw new IllegalArgumentException("Unsafe NPC file path");
        }
        return file;
    }

    private static NPCSkin readSkin(ConfigurationSection section) {
        if (section == null) {
            return NPCSkin.none();
        }
        String modeName = section.getString("mode", "NONE").toUpperCase(Locale.ROOT);
        NPCSkinMode mode;
        try {
            mode = NPCSkinMode.valueOf(modeName);
        } catch (IllegalArgumentException exception) {
            mode = NPCSkinMode.NONE;
        }
        return new NPCSkin(mode, section.getString("source", ""), section.getString("value", ""), section.getString("signature", ""));
    }

    private double readCooldown(Object value) {
        if (value instanceof String text && text.equalsIgnoreCase("disabled")) {
            return -1.0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return plugin.getConfig().getDouble("interaction.default-cooldown-seconds", 1.5D);
    }

    private void readEquipment(VirtualNPC npc, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                NPCEquipmentSlot slot = NPCEquipmentSlot.parse(key);
                Object raw = section.get(key);
                ItemStack stack;
                if (raw instanceof ItemStack itemStack) {
                    stack = itemStack;
                } else {
                    Material material = Material.matchMaterial(String.valueOf(raw));
                    stack = material == null ? null : new ItemStack(material);
                }
                npc.setEquipment(slot, stack);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void readActions(VirtualNPC npc, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String triggerKey : section.getKeys(false)) {
            NPCActionTrigger trigger = NPCActionTrigger.parse(triggerKey);
            List<NPCAction> actions = new ArrayList<>();
            List<?> list = section.getList(triggerKey, List.of());
            for (Object raw : list) {
                if (raw instanceof ConfigurationSection actionSection) {
                    actions.add(new NPCAction(actionSection.getString("type", "MESSAGE"), actionSection.getString("value", "")));
                } else if (raw instanceof java.util.Map<?, ?> map) {
                    Object type = map.get("type");
                    Object value = map.get("value");
                    actions.add(new NPCAction(String.valueOf(type == null ? "MESSAGE" : type), String.valueOf(value == null ? "" : value)));
                }
            }
            npc.setActions(trigger, actions);
        }
    }

    private static UUID parseUuid(String raw, String id) {
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.nameUUIDFromBytes(("AxoNPCs:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
