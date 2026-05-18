package org.axostudio.axonpcs.manager;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.NPCActionExecutor;
import org.axostudio.axonpcs.api.event.AxoNPCInteractEvent;
import org.axostudio.axonpcs.api.model.NPCAction;
import org.axostudio.axonpcs.api.model.NPCActionTrigger;
import org.axostudio.axonpcs.model.VirtualNPC;
import org.axostudio.axonpcs.util.ColorUtil;
import org.axostudio.axonpcs.util.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NPCActionManager {
    private final AxoNPCsPlugin plugin;
    private final Map<String, NPCActionExecutor> executors = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public NPCActionManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    public void register(String type, NPCActionExecutor executor) {
        executors.put(type.toUpperCase(Locale.ROOT), executor);
    }

    public List<String> types() {
        return executors.keySet().stream().sorted().toList();
    }

    public void handleInteract(Player player, VirtualNPC npc, NPCActionTrigger trigger) {
        AxoNPCInteractEvent event = new AxoNPCInteractEvent(player, npc, trigger);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        if (isCoolingDown(player, npc)) {
            long remaining = remainingMillis(player, npc);
            plugin.getMessageManager().send(player, "npc-cooldown", Map.of("seconds", String.format(Locale.US, "%.1f", remaining / 1000.0D)));
            return;
        }
        markCooldown(player, npc);
        for (NPCAction action : actionsFor(npc, trigger)) {
            NPCActionExecutor executor = executors.get(action.type().toUpperCase(Locale.ROOT));
            if (executor != null) {
                executor.execute(player, npc, replace(player, npc, action.value()));
            }
        }
    }

    private List<NPCAction> actionsFor(VirtualNPC npc, NPCActionTrigger trigger) {
        List<NPCAction> actions = new ArrayList<>();
        actions.addAll(npc.getActions(trigger));
        actions.addAll(npc.getActions(NPCActionTrigger.ANY));
        return actions;
    }

    private boolean isCoolingDown(Player player, VirtualNPC npc) {
        return remainingMillis(player, npc) > 0L;
    }

    private long remainingMillis(Player player, VirtualNPC npc) {
        if (npc.getInteractionCooldownSeconds() <= 0) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long until = cooldowns.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(npc.getId(), 0L);
        return Math.max(0L, until - now);
    }

    private void markCooldown(Player player, VirtualNPC npc) {
        if (npc.getInteractionCooldownSeconds() <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + (long) (npc.getInteractionCooldownSeconds() * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(npc.getId(), until);
    }

    private String replace(Player player, VirtualNPC npc, String value) {
        String replaced = value
                .replace("{player}", player.getName())
                .replace("{npc}", npc.getId())
                .replace("{world}", npc.getPosition().world());
        return PlaceholderUtil.apply(player, replaced, plugin.getConfig().getBoolean("language.placeholderapi", true));
    }

    private void registerDefaults() {
        register("MESSAGE", (player, npc, value) -> player.sendMessage(ColorUtil.parse(value)));
        register("PLAYER_COMMAND", (player, npc, value) -> player.performCommand(stripSlash(value)));
        register("COMMAND", (player, npc, value) -> player.performCommand(stripSlash(value)));
        register("CONSOLE_COMMAND", (player, npc, value) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(value)));
        register("SERVER", (player, npc, value) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(value)));
    }

    private static String stripSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
