package org.axostudio.axonpcs.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

public final class PlaceholderUtil {
    private PlaceholderUtil() {
    }

    public static String apply(OfflinePlayer player, String input, boolean enabled) {
        if (!enabled || input == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return input;
        }
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, input);
            return result instanceof String ? (String) result : input;
        } catch (ReflectiveOperationException exception) {
            return input;
        }
    }
}
