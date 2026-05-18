package org.axostudio.axonpcs.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("&?#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_HEX = Pattern.compile("&\\#([A-Fa-f0-9]{6})");
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"),
            Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );

    private ColorUtil() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(toMiniMessage(input));
    }

    public static String plain(String input) {
        return PlainTextComponentSerializer.plainText().serialize(parse(input));
    }

    public static NamedTextColor namedColor(String input, NamedTextColor fallback) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("off")) {
            return fallback;
        }
        NamedTextColor color = NamedTextColor.NAMES.value(input.toLowerCase(Locale.ROOT));
        return color == null ? fallback : color;
    }

    public static String toMiniMessage(String input) {
        Matcher legacyHex = LEGACY_HEX.matcher(input);
        String withHex = legacyHex.replaceAll("<#$1>");
        Matcher plainHex = HEX.matcher(withHex);
        withHex = plainHex.replaceAll("<#$1>");

        StringBuilder builder = new StringBuilder(withHex.length() + 16);
        for (int i = 0; i < withHex.length(); i++) {
            char current = withHex.charAt(i);
            if ((current == '&' || current == '§') && i + 1 < withHex.length()) {
                char code = Character.toLowerCase(withHex.charAt(++i));
                String tag = LEGACY_TAGS.get(code);
                if (tag != null) {
                    builder.append('<').append(tag).append('>');
                    continue;
                }
                builder.append(current).append(code);
                continue;
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
