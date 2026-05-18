package org.axostudio.axonpcs;

import org.axostudio.axonpcs.api.AxoNPCsAPI;
import org.axostudio.axonpcs.api.AxoNPCsAPIImpl;
import org.axostudio.axonpcs.api.AxoNPCsProvider;
import org.axostudio.axonpcs.command.AxoNPCsCommand;
import org.axostudio.axonpcs.command.NPCCommand;
import org.axostudio.axonpcs.command.PaperBasicCommandAdapter;
import org.axostudio.axonpcs.listener.NPCInteractListener;
import org.axostudio.axonpcs.listener.PlayerListener;
import org.axostudio.axonpcs.manager.MessageManager;
import org.axostudio.axonpcs.manager.NPCActionManager;
import org.axostudio.axonpcs.manager.NPCManager;
import org.axostudio.axonpcs.manager.NPCViewerManager;
import org.axostudio.axonpcs.manager.SkinManager;
import org.axostudio.axonpcs.packet.NPCPacketManager;
import org.axostudio.axonpcs.storage.NPCStorageManager;
import org.axostudio.axonpcs.util.ColorUtil;
import org.axostudio.axonpcs.util.PacketEventsGuard;
import org.axostudio.axonpcs.util.SchedulerUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;

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
    public void onEnable() {
        saveDefaultConfig();
        createDataDirectories();
        if (!isPacketEventsReady()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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
        PacketEventsGuard.reportViaVersionStatus(this);

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        api = new AxoNPCsAPIImpl(this);
        AxoNPCsProvider.register(api);
        Bukkit.getOnlinePlayers().forEach(player -> viewerManager.start(player));
        sendStartupBanner();
    }

    @Override
    public void onDisable() {
        if (viewerManager != null) {
            viewerManager.hideAll();
        }
        if (api != null) {
            AxoNPCsProvider.unregister(api);
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

    private boolean isPacketEventsReady() {
        if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            getLogger().severe("PacketEvents is required to use AxoNPCs client-side NPCs.");
            getLogger().severe("Install the external PacketEvents plugin 2.12.1+ and restart the server.");
            return false;
        }
        try {
            if (PacketEvents.getAPI() == null) {
                getLogger().severe("PacketEvents API is not initialized yet. Check plugin load order.");
                return false;
            }
        } catch (LinkageError error) {
            getLogger().severe("PacketEvents is enabled but its classes are not visible to AxoNPCs.");
            getLogger().severe("Check that the PacketEvents plugin jar is installed correctly.");
            return false;
        }
        return true;
    }

    private void sendStartupBanner() {
        String[] banner = {
                "   _____                  _______ ___________________         ",
                "  /  _  \\ ___  _______    \\      \\\\______   \\_   ___ \\  ______",
                " /  /_\\  \\\\  \\/  /  _ \\   /   |   \\|     ___/    \\  \\/ /  ___/",
                "/    |    \\>    <  <_> ) /    |    \\    |   \\     \\____\\___ \\ ",
                "\\____|__  /__/\\_ \\____/  \\____|__  /____|    \\______  /____  >",
                "        \\/      \\/               \\/                 \\/     \\/ "
        };
        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<light_purple>" + line));
        }
        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse(
                "<green>AxoNPCs se activo correctamente con <white>" + npcManager.all().size() + "</white> NPCs.</green>"
        ));
    }
}
