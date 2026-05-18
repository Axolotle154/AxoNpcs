package org.axostudio.axonpcs.util;

import org.bukkit.command.CommandSender;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission("axonpcs.admin")
                || sender.hasPermission("axonpcs.command.*")
                || sender.hasPermission(permission);
    }

    public static boolean hasNpc(CommandSender sender, String permission) {
        return has(sender, permission) || sender.hasPermission("axonpcs.command.npc.*");
    }
}
