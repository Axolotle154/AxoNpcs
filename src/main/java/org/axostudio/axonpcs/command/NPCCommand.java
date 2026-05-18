package org.axostudio.axonpcs.command;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.model.NPCPosition;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.ColorUtil;
import org.axostudio.axonpcs.util.IdValidator;
import org.axostudio.axonpcs.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class NPCCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "help", "create", "remove", "list", "info", "type", "displayname", "skin", "glowing", "collidable",
            "scale", "movehere", "moveto", "rotate", "nearby", "teleport", "action", "interactioncooldown"
    );

    private final AxoNPCsPlugin plugin;

    public NPCCommand(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!PermissionUtil.hasNpc(sender, permission(sub))) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        try {
            switch (sub) {
                case "create" -> create(sender, args);
                case "remove" -> remove(sender, args);
                case "list" -> list(sender, args);
                case "info" -> info(sender, args);
                case "type" -> type(sender, args);
                case "displayname" -> displayName(sender, args);
                case "skin" -> skin(sender, args);
                case "glowing" -> glowing(sender, args);
                case "collidable" -> collidable(sender, args);
                case "scale" -> scale(sender, args);
                case "movehere" -> moveHere(sender, args);
                case "moveto" -> moveTo(sender, args);
                case "rotate" -> rotate(sender, args);
                case "nearby" -> nearby(sender, args);
                case "teleport" -> teleport(sender, args);
                case "action" -> action(sender, args);
                case "interactioncooldown" -> interactionCooldown(sender, args);
                default -> plugin.getMessageManager().send(sender, "unknown-command");
            }
        } catch (NumberFormatException exception) {
            plugin.getMessageManager().send(sender, "invalid-usage", Map.of("usage", "/" + label + " help"));
        } catch (IllegalArgumentException exception) {
            plugin.getMessageManager().send(sender, "npc-invalid-id");
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/npc create <id> [--position <x y z>] [--world <world>] [--type <type>]");
            return;
        }
        String id = IdValidator.requireValid(args[1]);
        if (plugin.getNpcManager().exists(id)) {
            plugin.getMessageManager().send(sender, "npc-exists", Map.of("id", id));
            return;
        }
        Location base = sender instanceof Player player ? player.getLocation() : null;
        String worldName = base == null || base.getWorld() == null ? null : base.getWorld().getName();
        double x = base == null ? 0.0D : base.getX();
        double y = base == null ? 64.0D : base.getY();
        double z = base == null ? 0.0D : base.getZ();
        float yaw = base == null ? 0.0F : base.getYaw();
        float pitch = base == null ? 0.0F : base.getPitch();
        String type = "PLAYER";
        for (int i = 2; i < args.length; i++) {
            switch (args[i].toLowerCase(Locale.ROOT)) {
                case "--position" -> {
                    x = Double.parseDouble(args[++i]);
                    y = Double.parseDouble(args[++i]);
                    z = Double.parseDouble(args[++i]);
                }
                case "--world" -> worldName = args[++i];
                case "--type" -> type = args[++i].toUpperCase(Locale.ROOT);
                default -> {
                }
            }
        }
        World world = worldName == null ? Bukkit.getWorlds().getFirst() : Bukkit.getWorld(worldName);
        if (world == null) {
            usage(sender, "/npc create <id> --world <world>");
            return;
        }
        VirtualNPC npc = plugin.getNpcManager().create(id, new Location(world, x, y, z, yaw, pitch));
        npc.setType(type);
        plugin.getNpcManager().save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        plugin.getMessageManager().send(sender, "npc-created", Map.of("id", id));
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/npc remove <id>");
            return;
        }
        String id = IdValidator.normalize(args[1]);
        if (plugin.getNpcManager().delete(id)) {
            plugin.getMessageManager().send(sender, "npc-removed", Map.of("id", id));
        } else {
            plugin.getMessageManager().send(sender, "npc-not-found", Map.of("id", id));
        }
    }

    private void list(CommandSender sender, String[] args) {
        String typeFilter = null;
        String sort = "id";
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--type") && i + 1 < args.length) {
                typeFilter = args[++i].toUpperCase(Locale.ROOT);
            } else if (args[i].equalsIgnoreCase("--sort") && i + 1 < args.length) {
                sort = args[++i].toLowerCase(Locale.ROOT);
            }
        }
        List<VirtualNPC> npcs = new ArrayList<>(plugin.getNpcManager().all());
        if (typeFilter != null) {
            String finalTypeFilter = typeFilter;
            npcs.removeIf(npc -> !npc.getType().equalsIgnoreCase(finalTypeFilter));
        }
        Comparator<VirtualNPC> comparator = switch (sort) {
            case "type" -> Comparator.comparing(VirtualNPC::getType).thenComparing(VirtualNPC::getId);
            case "world" -> Comparator.comparing(npc -> npc.getPosition().world());
            default -> Comparator.comparing(VirtualNPC::getId);
        };
        npcs.sort(comparator);
        if (npcs.isEmpty()) {
            plugin.getMessageManager().send(sender, "npc-list-empty");
            return;
        }
        plugin.getMessageManager().send(sender, "npc-list-header", Map.of("count", String.valueOf(npcs.size())));
        for (VirtualNPC npc : npcs) {
            plugin.getMessageManager().send(sender, "npc-list-entry", placeholders(npc));
        }
    }

    private void info(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc != null) {
            plugin.getMessageManager().send(sender, "npc-info", placeholders(npc));
        }
    }

    private void type(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 3) {
            usage(sender, "/npc type <id> <type>");
            return;
        }
        npc.setType(args[2]);
        plugin.getNpcManager().save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        updated(sender, npc);
    }

    private void displayName(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null) {
            return;
        }
        if (args.length < 3) {
            String current = npc.getDisplayName() == null ? "@none" : npc.getDisplayName();
            sender.sendMessage(ColorUtil.parse("<gray>display-name:</gray> " + current));
            return;
        }
        String name = join(args, 2);
        npc.setDisplayName(name.equalsIgnoreCase("@none") ? null : name);
        plugin.getNpcManager().save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        updated(sender, npc);
    }

    private void skin(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 3) {
            usage(sender, "/npc skin <id> <skin|@none|@mirror>");
            return;
        }
        String skin = args[2];
        plugin.getMessageManager().send(sender, "npc-skin-loading", Map.of("skin", skin));
        plugin.getSkinManager().resolve(skin).thenAccept(optional -> {
            if (optional.isEmpty()) {
                sendLater(sender, "npc-skin-failed", Map.of("skin", skin));
                return;
            }
            plugin.getSchedulerUtil().runGlobal(() -> {
                npc.setSkin(optional.get());
                plugin.getNpcManager().save(npc);
                plugin.getViewerManager().refreshNPC(npc);
                sendLater(sender, "npc-updated", Map.of("id", npc.getId()));
            });
        });
    }

    private void glowing(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 3) {
            usage(sender, "/npc glowing <id> <color|off>");
            return;
        }
        npc.setGlowing(args[2]);
        plugin.getNpcManager().save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        updated(sender, npc);
    }

    private void collidable(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 3) {
            usage(sender, "/npc collidable <id> <true|false>");
            return;
        }
        npc.setCollidable(Boolean.parseBoolean(args[2]));
        plugin.getNpcManager().save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        updated(sender, npc);
    }

    private void scale(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 3) {
            usage(sender, "/npc scale <id> <factor>");
            return;
        }
        npc.setScale(Double.parseDouble(args[2]));
        plugin.getNpcManager().save(npc);
        plugin.getViewerManager().refreshNPC(npc);
        updated(sender, npc);
    }

    private void moveHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc != null) {
            plugin.getNpcManager().setLocation(npc.getId(), player.getLocation());
            updated(sender, npc);
        }
    }

    private void moveTo(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 5) {
            usage(sender, "/npc moveto <id> <x> <y> <z> [world]");
            return;
        }
        String worldName = args.length >= 6 ? args[5] : npc.getPosition().world();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            usage(sender, "/npc moveto <id> <x> <y> <z> [world]");
            return;
        }
        Location location = new Location(world, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]), npc.getPosition().yaw(), npc.getPosition().pitch());
        plugin.getNpcManager().setLocation(npc.getId(), location);
        updated(sender, npc);
    }

    private void rotate(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 4) {
            usage(sender, "/npc rotate <id> <yaw> <pitch>");
            return;
        }
        plugin.getNpcManager().setRotation(npc.getId(), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
        updated(sender, npc);
    }

    private void nearby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }
        double radius = args.length >= 2 ? Double.parseDouble(args[1]) : 16.0D;
        List<VirtualNPC> nearby = plugin.getNpcManager().all().stream()
                .filter(npc -> npc.getPosition().world().equals(player.getWorld().getName()))
                .filter(npc -> player.getLocation().distanceSquared(npc.getLocation()) <= radius * radius)
                .sorted(Comparator.comparingDouble(npc -> player.getLocation().distanceSquared(npc.getLocation())))
                .toList();
        if (nearby.isEmpty()) {
            plugin.getMessageManager().send(sender, "npc-list-empty");
            return;
        }
        plugin.getMessageManager().send(sender, "npc-list-header", Map.of("count", String.valueOf(nearby.size())));
        nearby.forEach(npc -> plugin.getMessageManager().send(sender, "npc-list-entry", placeholders(npc)));
    }

    private void teleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null) {
            return;
        }
        player.teleportAsync(npc.getLocation());
        plugin.getMessageManager().send(sender, "npc-teleported", Map.of("id", npc.getId()));
    }

    private void action(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 4) {
            usage(sender, "/npc action <id> <trigger> add|list|remove ...");
            return;
        }
        NPCActionTrigger trigger = NPCActionTrigger.parse(args[2]);
        String operation = args[3].toLowerCase(Locale.ROOT);
        switch (operation) {
            case "add" -> {
                if (args.length < 6) {
                    usage(sender, "/npc action <id> <trigger> add <type> <value>");
                    return;
                }
                npc.addAction(trigger, new NPCAction(args[4], join(args, 5)));
                plugin.getNpcManager().save(npc);
                plugin.getMessageManager().send(sender, "npc-action-added", Map.of("id", npc.getId()));
            }
            case "list" -> {
                List<NPCAction> actions = npc.getActions(trigger);
                if (actions.isEmpty()) {
                    plugin.getMessageManager().send(sender, "npc-action-list-empty");
                    return;
                }
                for (int i = 0; i < actions.size(); i++) {
                    NPCAction action = actions.get(i);
                    plugin.getMessageManager().send(sender, "npc-action-list-entry", Map.of(
                            "index", String.valueOf(i + 1),
                            "type", action.type(),
                            "value", action.value()
                    ));
                }
            }
            case "remove" -> {
                if (args.length < 5) {
                    usage(sender, "/npc action <id> <trigger> remove <index>");
                    return;
                }
                int index = Integer.parseInt(args[4]) - 1;
                if (npc.removeAction(trigger, index)) {
                    plugin.getNpcManager().save(npc);
                    plugin.getMessageManager().send(sender, "npc-action-removed", Map.of("id", npc.getId()));
                } else {
                    plugin.getMessageManager().send(sender, "npc-action-list-empty");
                }
            }
            default -> usage(sender, "/npc action <id> <trigger> add|list|remove ...");
        }
    }

    private void interactionCooldown(CommandSender sender, String[] args) {
        VirtualNPC npc = requireNPC(sender, args, 1);
        if (npc == null || args.length < 3) {
            usage(sender, "/npc interactioncooldown <id> <disabled|seconds>");
            return;
        }
        npc.setInteractionCooldownSeconds(args[2].equalsIgnoreCase("disabled") ? -1.0D : Double.parseDouble(args[2]));
        plugin.getNpcManager().save(npc);
        updated(sender, npc);
    }

    private VirtualNPC requireNPC(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            return null;
        }
        String id = IdValidator.normalize(args[index]);
        Optional<VirtualNPC> npc = plugin.getNpcManager().get(id);
        if (npc.isEmpty()) {
            plugin.getMessageManager().send(sender, "npc-not-found", Map.of("id", id));
            return null;
        }
        return npc.get();
    }

    private Map<String, String> placeholders(VirtualNPC npc) {
        NPCPosition pos = npc.getPosition();
        return Map.of(
                "id", npc.getId(),
                "type", npc.getType(),
                "world", pos.world(),
                "x", shortNumber(pos.x()),
                "y", shortNumber(pos.y()),
                "z", shortNumber(pos.z()),
                "yaw", shortNumber(pos.yaw()),
                "pitch", shortNumber(pos.pitch()),
                "viewers", String.valueOf(plugin.getViewerManager().viewerCount(npc))
        );
    }

    private void sendHelp(CommandSender sender) {
        for (int i = 1; i <= 5; i++) {
            plugin.getMessageManager().send(sender, "help-" + i);
        }
    }

    private void usage(CommandSender sender, String usage) {
        plugin.getMessageManager().send(sender, "invalid-usage", Map.of("usage", usage));
    }

    private void updated(CommandSender sender, VirtualNPC npc) {
        plugin.getMessageManager().send(sender, "npc-updated", Map.of("id", npc.getId()));
    }

    private void sendLater(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender instanceof Player player) {
            plugin.getSchedulerUtil().runEntity(player, () -> plugin.getMessageManager().send(player, key, placeholders));
        } else {
            plugin.getSchedulerUtil().runGlobal(() -> plugin.getMessageManager().send(sender, key, placeholders));
        }
    }

    private String permission(String sub) {
        return switch (sub) {
            case "create" -> "axonpcs.command.npc.create";
            case "remove" -> "axonpcs.command.npc.remove";
            case "list", "nearby" -> "axonpcs.command.npc.list";
            case "info" -> "axonpcs.command.npc.info";
            case "type" -> "axonpcs.command.npc.type";
            case "displayname" -> "axonpcs.command.npc.displayname";
            case "skin" -> "axonpcs.command.npc.skin";
            case "glowing", "collidable", "scale", "interactioncooldown" -> "axonpcs.command.npc.glowing";
            case "movehere", "moveto", "rotate" -> "axonpcs.command.npc.move";
            case "teleport" -> "axonpcs.command.npc.teleport";
            case "action" -> "axonpcs.command.npc.action";
            default -> "axonpcs.command.npc.*";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("nearby")) {
            return filter(plugin.getNpcManager().all().stream().map(VirtualNPC::getId).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("action")) {
            return filter(List.of("RIGHT_CLICK", "LEFT_CLICK", "ANY"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("action")) {
            return filter(List.of("add", "list", "remove"), args[3]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("skin")) {
            return filter(List.of("@none", "@mirror"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("glowing")) {
            return filter(List.of("off", "white", "green", "aqua", "red", "yellow", "blue", "gold", "light_purple"), args[2]);
        }
        return List.of();
    }

    private static String join(String[] args, int from) {
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static String shortNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
