package org.axostudio.axonpcs.command;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.migration.FancyNPCMigration;
import org.axostudio.axonpcs.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AxoNPCsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("version", "reload", "featureflags", "migrate");

    private final AxoNPCsPlugin plugin;

    public AxoNPCsCommand(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getMessageManager().send(sender, "invalid-usage", Map.of("usage", "/axonpcs <version|reload|featureflags|migrate>"));
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
                int count = plugin.getNpcManager().reload();
                plugin.getViewerManager().restartAll();
                plugin.getMessageManager().send(sender, "reload", Map.of("count", String.valueOf(count)));
                break;
            case "featureflags":
                if (!PermissionUtil.has(sender, "axonpcs.command.featureflags")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                plugin.getMessageManager().send(sender, "featureflags", Map.of("flags", featureFlags()));
                break;
            case "migrate":
                if (!PermissionUtil.has(sender, "axonpcs.command.migrate")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                migrate(sender, label, args);
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
            return filter(permittedSubcommands(sender), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate") && PermissionUtil.has(sender, "axonpcs.command.migrate")) {
            return filter(List.of("fancynpcs"), args[1]);
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("migrate") && args[1].equalsIgnoreCase("fancynpcs") && PermissionUtil.has(sender, "axonpcs.command.migrate")) {
            return filter(List.of("--overwrite"), args[args.length - 1]);
        }
        return List.of();
    }

    private List<String> permittedSubcommands(CommandSender sender) {
        return SUBCOMMANDS.stream()
                .filter(sub -> PermissionUtil.has(sender, permission(sub)))
                .toList();
    }

    private static String permission(String sub) {
        return switch (sub) {
            case "version" -> "axonpcs.command.version";
            case "reload" -> "axonpcs.command.reload";
            case "featureflags" -> "axonpcs.command.featureflags";
            case "migrate" -> "axonpcs.command.migrate";
            default -> "axonpcs.command.*";
        };
    }

    private void migrate(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("fancynpcs")) {
            plugin.getMessageManager().send(sender, "invalid-usage", Map.of("usage", "/" + label + " migrate fancynpcs [path] [--overwrite]"));
            return;
        }
        boolean overwrite = false;
        List<String> pathParts = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--overwrite")) {
                overwrite = true;
            } else {
                pathParts.add(args[i]);
            }
        }
        File input = pathParts.isEmpty() ? null : new File(String.join(" ", pathParts));
        try {
            FancyNPCMigration.Result result = new FancyNPCMigration(plugin).migrate(input, overwrite);
            plugin.getMessageManager().send(sender, "migrate-fancynpcs-success", Map.of(
                    "imported", String.valueOf(result.imported()),
                    "skipped", String.valueOf(result.skipped()),
                    "failed", String.valueOf(result.failed()),
                    "file", result.file().getAbsolutePath()
            ));
        } catch (RuntimeException exception) {
            plugin.getMessageManager().send(sender, "migrate-fancynpcs-failed", Map.of("error", exception.getMessage()));
        }
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
