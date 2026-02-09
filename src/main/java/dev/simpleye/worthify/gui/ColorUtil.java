package dev.simpleye.worthify.gui;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX = Pattern.compile("#([A-Fa-f0-9]{6})");

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }

        String withLegacy = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
        Matcher matcher = HEX.matcher(withLegacy);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
