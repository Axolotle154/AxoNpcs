package org.axostudio.axonpcs.migration;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.model.NPCPosition;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.packet.EntityIdAllocator;
import org.axostudio.axonpcs.util.IdValidator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class FancyNPCMigration {
    private final AxoNPCsPlugin plugin;

    public FancyNPCMigration(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    public static File defaultFile(AxoNPCsPlugin plugin) {
        File pluginsDirectory = plugin.getDataFolder().getParentFile();
        return new File(new File(pluginsDirectory, "FancyNpcs"), "npcs.yml");
    }

    public Result migrate(File input, boolean overwrite) {
        File file = resolveFile(input);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("FancyNpcs npcs.yml not found: " + file.getAbsolutePath());
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("npcs");
        if (root == null) {
            throw new IllegalArgumentException("FancyNpcs file does not contain an npcs section: " + file.getAbsolutePath());
        }

        FancyDefaults defaults = defaults(file);
        Set<String> idsInFile = new HashSet<>();
        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (String fancyKey : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(fancyKey);
            if (section == null) {
                failed++;
                continue;
            }
            try {
                VirtualNPC npc = convert(fancyKey, section, defaults, idsInFile);
                if (plugin.getNpcManager().importNPC(npc, overwrite)) {
                    imported++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException exception) {
                failed++;
                plugin.getLogger().log(Level.WARNING, "Could not migrate FancyNpcs NPC " + fancyKey, exception);
            }
        }

        if (imported > 0 && plugin.getViewerManager() != null) {
            plugin.getViewerManager().refreshAll();
        }
        return new Result(file, imported, skipped, failed);
    }

    private VirtualNPC convert(String fancyKey, ConfigurationSection section, FancyDefaults defaults, Set<String> idsInFile) {
        String id = uniqueId(safeId(section.getString("name", fancyKey)), idsInFile);
        UUID uuid = parseUuid(fancyKey, id);
        ConfigurationSection location = section.getConfigurationSection("location");
        NPCPosition position = new NPCPosition(
                location == null ? "world" : location.getString("world", "world"),
                location == null ? 0.0D : location.getDouble("x", 0.0D),
                location == null ? 64.0D : location.getDouble("y", 64.0D),
                location == null ? 0.0D : location.getDouble("z", 0.0D),
                (float) (location == null ? 0.0D : location.getDouble("yaw", 0.0D)),
                (float) (location == null ? 0.0D : location.getDouble("pitch", 0.0D))
        );

        VirtualNPC npc = new VirtualNPC(id, uuid, EntityIdAllocator.nextEntityId(), position);
        npc.setEnabled(section.getBoolean("enabled", section.getBoolean("spawnEntity", true)));
        npc.setType(section.getString("type", "PLAYER"));
        npc.setDisplayName(readDisplayName(section));
        npc.setSkin(readSkin(section.getConfigurationSection("skin")));
        npc.setGlowing(section.getBoolean("glowing", false) ? section.getString("glowingColor", "white") : "off");
        npc.setCollidable(section.getBoolean("collidable", false));
        npc.setScale(section.getDouble("scale", 1.0D));
        npc.setViewDistance(distanceOrDefault(section.getDouble("visibility_distance", -1.0D), defaults.visibilityDistance()));
        npc.setInteractionCooldownSeconds(section.getDouble("interactionCooldown", plugin.getConfig().getDouble("interaction.default-cooldown-seconds", 1.5D)));
        npc.setTurnToPlayer(section.getBoolean("turnToPlayer", false));
        npc.setTurnToPlayerDistance(distanceOrDefault(section.getDouble("turnToPlayerDistance", -1.0D), defaults.turnToPlayerDistance()));
        readEquipment(npc, section.getConfigurationSection("equipment"));
        readActions(npc, section.getConfigurationSection("actions"));
        return npc;
    }

    private File resolveFile(File input) {
        if (input == null) {
            return defaultFile(plugin);
        }
        if (input.isDirectory()) {
            return new File(input, "npcs.yml");
        }
        return input;
    }

    private FancyDefaults defaults(File npcsFile) {
        File configFile = new File(npcsFile.getParentFile(), "config.yml");
        if (!configFile.exists()) {
            return new FancyDefaults(
                    plugin.getConfig().getDouble("rendering.view-distance", 48.0D),
                    plugin.getConfig().getDouble("optimization.rotation.max-distance", 16.0D)
            );
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        return new FancyDefaults(
                config.getDouble("visibility_distance", plugin.getConfig().getDouble("rendering.view-distance", 48.0D)),
                config.getDouble("turn_to_player_distance", plugin.getConfig().getDouble("optimization.rotation.max-distance", 16.0D))
        );
    }

    private String readDisplayName(ConfigurationSection section) {
        if (section.contains("displayName")) {
            return section.getString("displayName", null);
        }
        return section.getString("name", null);
    }

    private NPCSkin readSkin(ConfigurationSection section) {
        if (section == null) {
            return NPCSkin.none();
        }
        if (section.getBoolean("mirrorSkin", false)) {
            return NPCSkin.mirror();
        }
        String value = firstString(section, "value", "texture", "textures.value", "data.value");
        String signature = firstString(section, "signature", "textures.signature", "data.signature");
        String source = firstString(section, "source", "name", "player", "owner", "identifier");
        if (!value.isBlank()) {
            return NPCSkin.texture(source.isBlank() ? "fancynpcs" : source, value, signature);
        }
        if (!source.isBlank() && !source.equalsIgnoreCase("@none")) {
            return NPCSkin.named(source);
        }
        return NPCSkin.none();
    }

    private void readEquipment(VirtualNPC npc, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(String.valueOf(section.get(key)));
            if (material == null) {
                continue;
            }
            try {
                npc.setEquipment(NPCEquipmentSlot.parse(key), new ItemStack(material));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void readActions(VirtualNPC npc, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String triggerKey : section.getKeys(false)) {
            List<NPCAction> actions = readActionValues(section.get(triggerKey));
            if (!actions.isEmpty()) {
                npc.setActions(NPCActionTrigger.parse(triggerKey), actions);
            }
        }
    }

    private List<NPCAction> readActionValues(Object raw) {
        List<NPCAction> actions = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object entry : (List<?>) raw) {
                readAction(entry, actions);
            }
            return actions;
        }
        if (raw instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) raw;
            if (section.contains("type") || section.contains("value") || section.contains("command") || section.contains("message")) {
                readAction(section, actions);
                return actions;
            }
            section.getKeys(false).stream()
                    .sorted(Comparator.comparingInt(FancyNPCMigration::actionIndex))
                    .map(section::get)
                    .forEach(entry -> readAction(entry, actions));
        }
        return actions;
    }

    private void readAction(Object raw, List<NPCAction> actions) {
        if (raw instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) raw;
            String type = normalizeActionType(section.getString("type", "MESSAGE"));
            String value = firstString(section, "value", "command", "message", "text");
            actions.add(new NPCAction(type, value));
        } else if (raw instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) raw;
            String type = normalizeActionType(String.valueOf(valueOf(map, "type", "MESSAGE")));
            Object value = valueOf(map, "value", valueOf(map, "command", valueOf(map, "message", valueOf(map, "text", ""))));
            actions.add(new NPCAction(type, String.valueOf(value)));
        }
    }

    private static String normalizeActionType(String input) {
        String normalized = input == null ? "" : input.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "CONSOLE", "CONSOLE_COMMAND", "SERVER", "SERVER_COMMAND" -> "CONSOLE_COMMAND";
            case "COMMAND", "PLAYER_COMMAND", "PLAYER_COMMAND_AS_OP" -> "PLAYER_COMMAND";
            case "SEND_MESSAGE", "TEXT", "MESSAGE" -> "MESSAGE";
            default -> normalized.isBlank() ? "MESSAGE" : normalized;
        };
    }

    private static Object valueOf(Map<?, ?> map, String key, Object fallback) {
        Object value = map.get(key);
        if (value != null) {
            return value;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && key.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private static String firstString(ConfigurationSection section, String... paths) {
        for (String path : paths) {
            String value = section.getString(path, "");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static double distanceOrDefault(double value, double fallback) {
        return value > 0.0D ? value : fallback;
    }

    private static String safeId(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9_-]+", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "fancynpc";
        }
        if (normalized.length() > 48) {
            normalized = normalized.substring(0, 48);
        }
        if (!IdValidator.isValid(normalized)) {
            normalized = "fancynpc";
        }
        return normalized;
    }

    private static String uniqueId(String base, Set<String> used) {
        String candidate = base;
        int index = 2;
        while (used.contains(candidate)) {
            String suffix = "-" + index++;
            int maxBaseLength = 48 - suffix.length();
            candidate = base.substring(0, Math.min(base.length(), maxBaseLength)) + suffix;
        }
        used.add(candidate);
        return candidate;
    }

    private static UUID parseUuid(String raw, String id) {
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.nameUUIDFromBytes(("FancyNpcs:" + id).getBytes(StandardCharsets.UTF_8));
    }

    private static int actionIndex(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private record FancyDefaults(double visibilityDistance, double turnToPlayerDistance) {
    }

    public record Result(File file, int imported, int skipped, int failed) {
    }
}
