package org.axostudio.axonpcs.listener;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerListener implements Listener {
    private final AxoNPCsPlugin plugin;

    public PlayerListener(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getViewerManager().start(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getViewerManager().stop(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.getSchedulerUtil().runEntityDelayed(event.getPlayer(), () -> {
            plugin.getViewerManager().hideAll(event.getPlayer());
            plugin.getViewerManager().tick(event.getPlayer());
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        plugin.getSchedulerUtil().runEntityDelayed(event.getPlayer(), () -> plugin.getViewerManager().tick(event.getPlayer()), 1L);
    }
}
