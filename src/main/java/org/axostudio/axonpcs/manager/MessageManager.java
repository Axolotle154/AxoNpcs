package org.axostudio.axonpcs.manager;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.util.ColorUtil;
import org.axostudio.axonpcs.util.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageManager {
    private final AxoNPCsPlugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private String fallback = "en";

    public MessageManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        languages.clear();
        fallback = plugin.getConfig().getString("language.fallback", "en").toLowerCase(Locale.ROOT);
        File directory = new File(plugin.getDataFolder(), "languages");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Could not create languages directory");
        }
        for (String lang : new String[]{"en", "es", "ru"}) {
            plugin.saveResource("languages/" + lang + ".yml", false);
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            languages.put(name, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(component(sender, key, placeholders, true));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public Component component(CommandSender sender, String key, Map<String, String> placeholders, boolean prefix) {
        String language = language(sender);
        String raw = raw(language, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (sender instanceof Player player) {
            raw = PlaceholderUtil.apply(player, raw, plugin.getConfig().getBoolean("language.placeholderapi", true));
        }
        String prefixText = prefix ? raw(language, "prefix") : "";
        return ColorUtil.parse(prefixText + raw);
    }

    public String raw(CommandSender sender, String key) {
        return raw(language(sender), key);
    }

    private String raw(String language, String key) {
        YamlConfiguration selected = languages.getOrDefault(language, languages.get(fallback));
        String value = selected == null ? null : selected.getString(key);
        if (value == null && !fallback.equals(language)) {
            YamlConfiguration fallbackConfig = languages.get(fallback);
            value = fallbackConfig == null ? null : fallbackConfig.getString(key);
        }
        return value == null ? key : value;
    }

    private String language(CommandSender sender) {
        if (!(sender instanceof Player player) || !plugin.getConfig().getBoolean("language.auto-detect-client", true)) {
            return fallback;
        }
        String locale = player.getLocale();
        if (locale == null || locale.isBlank()) {
            return fallback;
        }
        String language = locale.toLowerCase(Locale.ROOT).split("[_-]", 2)[0];
        return languages.containsKey(language) ? language : fallback;
    }
}
