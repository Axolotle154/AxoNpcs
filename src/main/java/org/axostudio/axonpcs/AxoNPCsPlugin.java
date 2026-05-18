package org.axostudio.axonpcs;

import org.axostudio.axonpcs.api.AxoNPCsAPI;
import org.axostudio.axonpcs.api.AxoNPCsAPIImpl;
import org.axostudio.axonpcs.api.AxoNPCsProvider;
import org.axostudio.axonpcs.command.AxoNPCsCommand;
import org.axostudio.axonpcs.command.NPCCommand;
import org.axostudio.axonpcs.command.PaperBasicCommandAdapter;
import org.axostudio.axonpcs.listener.PlayerListener;
import org.axostudio.axonpcs.manager.MessageManager;
import org.axostudio.axonpcs.manager.NPCActionManager;
import org.axostudio.axonpcs.manager.NPCManager;
import org.axostudio.axonpcs.manager.NPCViewerManager;
import org.axostudio.axonpcs.manager.SkinManager;
import org.axostudio.axonpcs.packet.PacketBackend;
import org.axostudio.axonpcs.packet.PacketBackendFactory;
import org.axostudio.axonpcs.storage.NPCStorageManager;
import org.axostudio.axonpcs.util.ColorUtil;
import org.axostudio.axonpcs.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;

public final class AxoNPCsPlugin extends JavaPlugin {
    private SchedulerUtil schedulerUtil;
    private MessageManager messageManager;
    private NPCStorageManager storageManager;
    private NPCManager npcManager;
    private PacketBackend packetManager;
    private NPCViewerManager viewerManager;
    private NPCActionManager actionManager;
    private SkinManager skinManager;
    private AxoNPCsAPI api;
    private volatile boolean shuttingDown;

    @Override
    public void onEnable() {
        shuttingDown = false;
        saveDefaultConfig();
        createDataDirectories();

        schedulerUtil = new SchedulerUtil(this);
        messageManager = new MessageManager(this);
        messageManager.load();
        storageManager = new NPCStorageManager(this);
        skinManager = new SkinManager(this);
        skinManager.init();
        npcManager = new NPCManager(this, storageManager);
        packetManager = PacketBackendFactory.create(this);
        if (!packetManager.enable()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Using packet backend: " + packetManager.name());
        viewerManager = new NPCViewerManager(this);
        actionManager = new NPCActionManager(this);

        npcManager.reload();

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        api = new AxoNPCsAPIImpl(this);
        AxoNPCsProvider.register(api);
        Bukkit.getOnlinePlayers().forEach(player -> viewerManager.start(player));
        sendStartupBanner();
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        if (viewerManager != null) {
            viewerManager.shutdownNow();
        }
        if (api != null) {
            AxoNPCsProvider.unregister(api);
            api = null;
        }
        if (packetManager != null) {
            packetManager.disable();
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown || !isEnabled();
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

    public PacketBackend getPacketManager() {
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
        registerCommand(
                "axonpcs",
                "AxoNPCs administrative commands.",
                Collections.singleton("axonpc"),
                new PaperBasicCommandAdapter("axonpcs", axoNPCsCommand, axoNPCsCommand)
        );

        NPCCommand npcCommand = new NPCCommand(this);
        registerCommand(
                "npc",
                "NPC management commands.",
                Collections.emptyList(),
                new PaperBasicCommandAdapter("npc", npcCommand, npcCommand)
        );
    }

    private void sendStartupBanner() {
        String[] banner = {
                "   _____                  _______                        ",
                "  /  _  \\ ___  _______    \\      \\ ______   ____   ______",
                " /  /_\\  \\\\  \\/  /  _ \\   /   |   \\\\____ \\_/ ___\\ /  ___/",
                "/    |    \\>    <  <_> ) /    |    \\  |_> >  \\___ \\___ \\ ",
                "\\____|__  /__/\\_ \\____/  \\____|__  /   __/ \\___  >____  >",
                "        \\/      \\/               \\/|__|        \\/     \\/ "
        };
        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<light_purple>" + line));
        }
        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse(
                "<green>AxoNPCs se activo correctamente con <white>" + npcManager.all().size() + "</white> NPCs.</green>"
        ));
    }
}
