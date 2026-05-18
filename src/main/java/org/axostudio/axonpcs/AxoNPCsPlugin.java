package org.axostudio.axonpcs;

import org.axostudio.axonpcs.api.AxoNPCsAPI;
import org.axostudio.axonpcs.api.AxoNPCsAPIImpl;
import org.axostudio.axonpcs.api.AxoNPCsProvider;
import org.axostudio.axonpcs.command.AxoNPCsCommand;
import org.axostudio.axonpcs.command.NPCCommand;
import org.axostudio.axonpcs.listener.NPCInteractListener;
import org.axostudio.axonpcs.listener.PlayerListener;
import org.axostudio.axonpcs.manager.MessageManager;
import org.axostudio.axonpcs.manager.NPCActionManager;
import org.axostudio.axonpcs.manager.NPCManager;
import org.axostudio.axonpcs.manager.NPCViewerManager;
import org.axostudio.axonpcs.manager.SkinManager;
import org.axostudio.axonpcs.packet.NPCPacketManager;
import org.axostudio.axonpcs.storage.NPCStorageManager;
import org.axostudio.axonpcs.util.SchedulerUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class AxoNPCsPlugin extends JavaPlugin {
    private SchedulerUtil schedulerUtil;
    private MessageManager messageManager;
    private NPCStorageManager storageManager;
    private NPCManager npcManager;
    private NPCPacketManager packetManager;
    private NPCViewerManager viewerManager;
    private NPCActionManager actionManager;
    private SkinManager skinManager;
    private AxoNPCsAPI api;
    private NPCInteractListener packetListener;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createDataDirectories();

        schedulerUtil = new SchedulerUtil(this);
        messageManager = new MessageManager(this);
        messageManager.load();
        storageManager = new NPCStorageManager(this);
        skinManager = new SkinManager(this);
        skinManager.init();
        npcManager = new NPCManager(this, storageManager);
        packetManager = new NPCPacketManager(this);
        viewerManager = new NPCViewerManager(this);
        actionManager = new NPCActionManager(this);

        npcManager.reload();
        packetListener = new NPCInteractListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener, PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().init();

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        api = new AxoNPCsAPIImpl(this);
        AxoNPCsProvider.register(api);
        Bukkit.getOnlinePlayers().forEach(player -> viewerManager.start(player));
        getLogger().info("AxoNPCs enabled with " + npcManager.all().size() + " NPCs.");
    }

    @Override
    public void onDisable() {
        if (viewerManager != null) {
            viewerManager.hideAll();
        }
        if (api != null) {
            AxoNPCsProvider.unregister(api);
        }
        try {
            PacketEvents.getAPI().terminate();
        } catch (RuntimeException ignored) {
        }
    }

    public SchedulerUtil getSchedulerUtil() {
        return schedulerUtil;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public NPCPacketManager getPacketManager() {
        return packetManager;
    }

    public NPCViewerManager getViewerManager() {
        return viewerManager;
    }

    public NPCActionManager getActionManager() {
        return actionManager;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    private void createDataDirectories() {
        for (String path : new String[]{"languages", "logs", "skins", "npcs"}) {
            File file = new File(getDataFolder(), path);
            if (!file.exists() && !file.mkdirs()) {
                getLogger().warning("Could not create " + path + " directory");
            }
        }
        saveResource("version.yml", false);
    }

    private void registerCommands() {
        AxoNPCsCommand axoNPCsCommand = new AxoNPCsCommand(this);
        PluginCommand axonpcs = getCommand("axonpcs");
        if (axonpcs != null) {
            axonpcs.setExecutor(axoNPCsCommand);
            axonpcs.setTabCompleter(axoNPCsCommand);
        }

        NPCCommand npcCommand = new NPCCommand(this);
        PluginCommand npc = getCommand("npc");
        if (npc != null) {
            npc.setExecutor(npcCommand);
            npc.setTabCompleter(npcCommand);
        }
    }
}
