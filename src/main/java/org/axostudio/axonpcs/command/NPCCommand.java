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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class NPCCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "help", "create", "remove", "list", "info", "type", "displayname", "skin", "glowing", "collidable",
            "scale", "movehere", "moveto", "rotate", "nearby", "teleport", "action", "interactioncooldown"
    );
    private static final List<String> CREATE_OPTIONS = List.of("--position", "--world", "--type");
    private static final List<String> LIST_OPTIONS = List.of("--type", "--sort");
    private static final List<String> NPC_TYPES = List.of("PLAYER");
    private static final List<String> SORT_TYPES = List.of("id", "type", "world");
    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");
    private static final List<String> SKIN_VALUES = List.of("@none", "@mirror");
    private static final List<String> GLOWING_VALUES = List.of(
            "off", "white", "green", "aqua", "red", "yellow", "blue", "gold", "gray", "dark_gray",
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "light_purple", "pink"
    );
    private static final List<String> SCALE_VALUES = List.of("0.5", "1", "1.5", "2");
    private static final List<String> RADIUS_VALUES = List.of("8", "16", "32", "48", "64");
    private static final List<String> COOLDOWN_VALUES = List.of("disabled", "0", "1", "1.5", "3", "5");
    private static final List<String> ACTION_TRIGGERS = List.of("RIGHT_CLICK", "LEFT_CLICK", "ANY");
    private static final List<String> ACTION_OPERATIONS = List.of("add", "list", "remove");
    private static final List<String> ACTION_VALUE_HINTS = List.of("{player}", "{npc}", "{world}");

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
                case "create":
                    create(sender, args);
                    break;
                case "remove":
                    remove(sender, args);
                    break;
                case "list":
                    list(sender, args);
                    break;
                case "info":
                    info(sender, args);
                    break;
                case "type":
                    type(sender, args);
                    break;
                case "displayname":
                    displayName(sender, args);
                    break;
                case "skin":
                    skin(sender, args);
                    break;
                case "glowing":
                    glowing(sender, args);
                    break;
                case "collidable":
                    collidable(sender, args);
                    break;
                case "scale":
                    scale(sender, args);
                    break;
                case "movehere":
                    moveHere(sender, args);
                    break;
                case "moveto":
                    moveTo(sender, args);
                    break;
                case "rotate":
                    rotate(sender, args);
                    break;
                case "nearby":
                    nearby(sender, args);
                    break;
                case "teleport":
                    teleport(sender, args);
                    break;
                case "action":
                    action(sender, args);
                    break;
                case "interactioncooldown":
                    interactionCooldown(sender, args);
                    break;
                default:
                    plugin.getMessageManager().send(sender, "unknown-command");
                    break;
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
        Player playerSender = sender instanceof Player ? (Player) sender : null;
        Location base = playerSender == null ? null : playerSender.getLocation();
        String worldName = base == null || base.getWorld() == null ? null : base.getWorld().getName();
        double x = base == null ? 0.0D : base.getX();
        double y = base == null ? 64.0D : base.getY();
        double z = base == null ? 0.0D : base.getZ();
        float yaw = base == null ? 0.0F : base.getYaw();
        float pitch = base == null ? 0.0F : base.getPitch();
        String type = "PLAYER";
        for (int i = 2; i < args.length; i++) {
            switch (args[i].toLowerCase(Locale.ROOT)) {
                case "--position":
                    x = Double.parseDouble(args[++i]);
                    y = Double.parseDouble(args[++i]);
                    z = Double.parseDouble(args[++i]);
                    break;
                case "--world":
                    worldName = args[++i];
                    break;
                case "--type":
                    type = args[++i].toUpperCase(Locale.ROOT);
                    break;
                default:
                    break;
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
        Comparator<VirtualNPC> comparator;
        switch (sort) {
            case "type":
                comparator = Comparator.comparing(VirtualNPC::getType).thenComparing(VirtualNPC::getId);
                break;
            case "world":
                comparator = Comparator.comparing(npc -> npc.getPosition().world());
                break;
            default:
                comparator = Comparator.comparing(VirtualNPC::getId);
                break;
        }
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
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }
        Player player = (Player) sender;
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
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }
        Player player = (Player) sender;
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
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return;
        }
        Player player = (Player) sender;
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
            case "add":
                if (args.length < 6) {
                    usage(sender, "/npc action <id> <trigger> add <type> <value>");
                    return;
                }
                npc.addAction(trigger, new NPCAction(args[4], join(args, 5)));
                plugin.getNpcManager().save(npc);
                plugin.getMessageManager().send(sender, "npc-action-added", Map.of("id", npc.getId()));
                break;
            case "list":
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
                break;
            case "remove":
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
                break;
            default:
                usage(sender, "/npc action <id> <trigger> add|list|remove ...");
                break;
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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getSchedulerUtil().runEntity(player, () -> plugin.getMessageManager().send(player, key, placeholders));
        } else {
            plugin.getSchedulerUtil().runGlobal(() -> plugin.getMessageManager().send(sender, key, placeholders));
        }
    }

    private String permission(String sub) {
        switch (sub) {
            case "create":
                return "axonpcs.command.npc.create";
            case "remove":
                return "axonpcs.command.npc.remove";
            case "list":
            case "nearby":
                return "axonpcs.command.npc.list";
            case "info":
                return "axonpcs.command.npc.info";
            case "type":
                return "axonpcs.command.npc.type";
            case "displayname":
                return "axonpcs.command.npc.displayname";
            case "skin":
                return "axonpcs.command.npc.skin";
            case "glowing":
            case "collidable":
            case "scale":
            case "interactioncooldown":
                return "axonpcs.command.npc.glowing";
            case "movehere":
            case "moveto":
            case "rotate":
                return "axonpcs.command.npc.move";
            case "teleport":
                return "axonpcs.command.npc.teleport";
            case "action":
                return "axonpcs.command.npc.action";
            default:
                return "axonpcs.command.npc.*";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return permittedSubcommands(sender);
        }
        if (args.length == 1) {
            return filter(permittedSubcommands(sender), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sub.equals("help") && !PermissionUtil.hasNpc(sender, permission(sub))) {
            return List.of();
        }

        switch (sub) {
            case "help":
                return args.length == 2 ? filter(List.of("1", "2", "3", "4", "5"), args[1]) : List.of();
            case "create":
                return completeCreate(sender, args);
            case "list":
                return completeList(args);
            case "nearby":
                return args.length == 2 ? filter(RADIUS_VALUES, args[1]) : List.of();
            case "action":
                return completeAction(args);
            case "type":
                return completeNpcThen(args, 2, npcTypes());
            case "displayname":
                return completeNpcThen(args, 2, List.of("@none"));
            case "skin":
                return completeNpcThen(args, 2, skinSuggestions());
            case "glowing":
                return completeNpcThen(args, 2, GLOWING_VALUES);
            case "collidable":
                return completeNpcThen(args, 2, BOOLEAN_VALUES);
            case "scale":
                return completeNpcThen(args, 2, SCALE_VALUES);
            case "moveto":
                return completeMoveTo(sender, args);
            case "rotate":
                return completeRotate(sender, args);
            case "interactioncooldown":
                return completeNpcThen(args, 2, COOLDOWN_VALUES);
            case "remove":
            case "info":
            case "movehere":
            case "teleport":
                return args.length == 2 ? filter(npcIds(), args[1]) : List.of();
            default:
                return List.of();
        }
    }

    private List<String> completeCreate(CommandSender sender, String[] args) {
        int positionIndex = valueIndexForCurrent(args, "--position", 3);
        if (positionIndex >= 0) {
            return filter(positionSuggestion(sender, positionIndex), current(args));
        }
        if (valueIndexForCurrent(args, "--world", 1) == 0) {
            return filter(worldNames(), current(args));
        }
        if (valueIndexForCurrent(args, "--type", 1) == 0) {
            return filter(npcTypes(), current(args));
        }
        if (args.length >= 3) {
            return filter(unusedOptions(args, CREATE_OPTIONS), current(args));
        }
        return List.of();
    }

    private List<String> completeList(String[] args) {
        if (valueIndexForCurrent(args, "--type", 1) == 0) {
            return filter(npcTypes(), current(args));
        }
        if (valueIndexForCurrent(args, "--sort", 1) == 0) {
            return filter(SORT_TYPES, current(args));
        }
        return filter(unusedOptions(args, LIST_OPTIONS), current(args));
    }

    private List<String> completeAction(String[] args) {
        if (args.length == 2) {
            return filter(npcIds(), args[1]);
        }
        if (args.length == 3) {
            return filter(ACTION_TRIGGERS, args[2]);
        }
        if (args.length == 4) {
            return filter(ACTION_OPERATIONS, args[3]);
        }
        String operation = args[3].toLowerCase(Locale.ROOT);
        if (operation.equals("add")) {
            if (args.length == 5) {
                return filter(plugin.getActionManager().types(), args[4]);
            }
            if (args.length >= 6) {
                return filter(actionValueSuggestions(args[4]), current(args));
            }
        }
        if (operation.equals("remove") && args.length == 5) {
            return filter(actionIndexes(args[1], args[2]), args[4]);
        }
        return List.of();
    }

    private List<String> completeMoveTo(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(npcIds(), args[1]);
        }
        if (args.length >= 3 && args.length <= 5) {
            return filter(positionSuggestion(sender, args.length - 3), current(args));
        }
        if (args.length == 6) {
            return filter(worldNames(), args[5]);
        }
        return List.of();
    }

    private List<String> completeRotate(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(npcIds(), args[1]);
        }
        if (args.length == 3) {
            return filter(yawSuggestions(sender), args[2]);
        }
        if (args.length == 4) {
            return filter(pitchSuggestions(sender), args[3]);
        }
        return List.of();
    }

    private List<String> completeNpcThen(String[] args, int valueIndex, List<String> values) {
        if (args.length == 2) {
            return filter(npcIds(), args[1]);
        }
        if (args.length == valueIndex + 1) {
            return filter(values, args[valueIndex]);
        }
        return List.of();
    }

    private List<String> permittedSubcommands(CommandSender sender) {
        return SUBCOMMANDS.stream()
                .filter(sub -> sub.equals("help") || PermissionUtil.hasNpc(sender, permission(sub)))
                .toList();
    }

    private List<String> npcIds() {
        return plugin.getNpcManager().all().stream().map(VirtualNPC::getId).toList();
    }

    private List<String> npcTypes() {
        Set<String> types = new LinkedHashSet<>(NPC_TYPES);
        plugin.getNpcManager().all().stream()
                .map(VirtualNPC::getType)
                .filter(type -> type != null && !type.isBlank())
                .map(type -> type.toUpperCase(Locale.ROOT))
                .forEach(types::add);
        return List.copyOf(types);
    }

    private List<String> worldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }

    private List<String> skinSuggestions() {
        Set<String> values = new LinkedHashSet<>(SKIN_VALUES);
        Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(values::add);
        return List.copyOf(values);
    }

    private List<String> actionIndexes(String npcId, String triggerValue) {
        Optional<VirtualNPC> npc = plugin.getNpcManager().get(npcId);
        if (npc.isEmpty()) {
            return List.of();
        }
        NPCActionTrigger trigger = NPCActionTrigger.parse(triggerValue);
        List<String> indexes = new ArrayList<>();
        for (int i = 1; i <= npc.get().getActions(trigger).size(); i++) {
            indexes.add(String.valueOf(i));
        }
        return indexes;
    }

    private List<String> actionValueSuggestions(String type) {
        String normalized = type.toUpperCase(Locale.ROOT);
        if (normalized.contains("COMMAND") || normalized.equals("SERVER")) {
            return List.of("say", "spawn", "warp", "{player}", "{npc}", "{world}");
        }
        if (normalized.equals("MESSAGE")) {
            return List.of("<green>Hello", "&aHello", "&#FFAA00Hello", "{player}", "{npc}", "{world}");
        }
        return ACTION_VALUE_HINTS;
    }

    private List<String> positionSuggestion(CommandSender sender, int index) {
        if (sender instanceof Player player) {
            Location location = player.getLocation();
            return switch (index) {
                case 0 -> List.of(shortNumber(location.getX()));
                case 1 -> List.of(shortNumber(location.getY()));
                case 2 -> List.of(shortNumber(location.getZ()));
                default -> List.of();
            };
        }
        return switch (index) {
            case 0, 2 -> List.of("0");
            case 1 -> List.of("64");
            default -> List.of();
        };
    }

    private List<String> yawSuggestions(CommandSender sender) {
        if (sender instanceof Player player) {
            return List.of(shortNumber(player.getLocation().getYaw()), "0", "90", "180", "-90");
        }
        return List.of("0", "90", "180", "-90");
    }

    private List<String> pitchSuggestions(CommandSender sender) {
        if (sender instanceof Player player) {
            return List.of(shortNumber(player.getLocation().getPitch()), "0", "45", "-45");
        }
        return List.of("0", "45", "-45");
    }

    private static List<String> unusedOptions(String[] args, List<String> options) {
        Set<String> used = new LinkedHashSet<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                used.add(arg.toLowerCase(Locale.ROOT));
            }
        }
        return options.stream().filter(option -> !used.contains(option.toLowerCase(Locale.ROOT))).toList();
    }

    private static int valueIndexForCurrent(String[] args, String option, int valueCount) {
        int current = args.length - 1;
        for (int i = 0; i < current; i++) {
            if (!args[i].equalsIgnoreCase(option)) {
                continue;
            }
            int consumed = 0;
            for (int j = i + 1; j < current && !args[j].startsWith("--"); j++) {
                consumed++;
            }
            if (consumed < valueCount && current == i + 1 + consumed) {
                return consumed;
            }
        }
        return -1;
    }

    private static String current(String[] args) {
        return args.length == 0 ? "" : args[args.length - 1];
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
