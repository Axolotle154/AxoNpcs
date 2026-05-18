package org.axostudio.axonpcs.command;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AxoNPCsCommand implements CommandExecutor, TabCompleter {
    private final AxoNPCsPlugin plugin;

    public AxoNPCsCommand(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getMessageManager().send(sender, "invalid-usage", Map.of("usage", "/axonpcs <version|reload|featureflags>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "version":
                if (!PermissionUtil.has(sender, "axonpcs.command.version")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                plugin.getMessageManager().send(sender, "version", Map.of(
                        "version", plugin.getPluginMeta().getVersion(),
                        "server", plugin.getServer().getVersion()
                ));
                break;
            case "reload":
                if (!PermissionUtil.has(sender, "axonpcs.command.reload")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getMessageManager().load();
                plugin.getViewerManager().hideAll();
                int count = plugin.getNpcManager().reload();
                plugin.getViewerManager().refreshAll();
                plugin.getMessageManager().send(sender, "reload", Map.of("count", String.valueOf(count)));
                break;
            case "featureflags":
                if (!PermissionUtil.has(sender, "axonpcs.command.featureflags")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                plugin.getMessageManager().send(sender, "featureflags", Map.of("flags", featureFlags()));
                break;
            default:
                plugin.getMessageManager().send(sender, "unknown-command");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("version", "reload", "featureflags"), args[0]);
        }
        return List.of();
    }

    private String featureFlags() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("feature-flags");
        if (section == null) {
            return "";
        }
        List<String> flags = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            flags.add(key + "=" + section.getBoolean(key));
        }
        return String.join(", ", flags);
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(lower)).toList();
    }
}
