package org.axostudio.axonpcs.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;
import java.util.Map;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
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
        StringBuilder builder = new StringBuilder(input.length() + 16);
        boolean insideMiniMessageTag = false;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '<') {
                insideMiniMessageTag = true;
                builder.append(current);
                continue;
            }
            if (current == '>') {
                insideMiniMessageTag = false;
                builder.append(current);
                continue;
            }
            if (!insideMiniMessageTag && (current == '&' || current == '§') && i + 1 < input.length()) {
                if (input.charAt(i + 1) == '#' && hasHexColor(input, i + 2)) {
                    builder.append("<#").append(input, i + 2, i + 8).append('>');
                    i += 7;
                    continue;
                }
                char code = Character.toLowerCase(input.charAt(i + 1));
                String tag = LEGACY_TAGS.get(code);
                if (tag != null) {
                    builder.append('<').append(tag).append('>');
                    i++;
                    continue;
                }
                builder.append(current).append(input.charAt(i + 1));
                i++;
                continue;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static boolean hasHexColor(String input, int start) {
        if (start + 6 > input.length()) {
            return false;
        }
        for (int index = start; index < start + 6; index++) {
            char value = input.charAt(index);
            boolean digit = value >= '0' && value <= '9';
            boolean lower = value >= 'a' && value <= 'f';
            boolean upper = value >= 'A' && value <= 'F';
            if (!digit && !lower && !upper) {
                return false;
            }
        }
        return true;
    }
}
