package dev.simpleye.worthify.compat;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MaterialResolver {

    private final ServerVersion serverVersion;
    private final Map<String, String> aliases;

    public MaterialResolver(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
        this.aliases = buildAliases(serverVersion);
    }

    public Material resolve(String input) {
        if (input == null) {
            return null;
        }

        String key = normalize(input);
        if (key.isEmpty()) {
            return null;
        }

        Material direct = Material.matchMaterial(key);
        if (direct != null) {
            return direct;
        }

        String aliased = aliases.get(key);
        if (aliased != null) {
            return Material.matchMaterial(aliased);
        }

        return null;
    }

    public String getAliasTarget(String input) {
        if (input == null) {
            return null;
        }
        return aliases.get(normalize(input));
    }

    private static String normalize(String s) {
        return s.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> buildAliases(ServerVersion v) {
        if (v == null) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();

// items remapper Between Versions to yk fix the price.yml at some versions without a confilect
        map.put("JACK_OLANTERN", "JACK_O_LANTERN");
        map.put("MINECART_WITH_CHEST", "CHEST_MINECART");
        map.put("MINECART_WITH_FURNACE", "FURNACE_MINECART");
        map.put("MINECART_WITH_HOPPER", "HOPPER_MINECART");
        map.put("MINECART_WITH_TNT", "TNT_MINECART");
        map.put("MINECART_WITH_COMMAND_BLOCK", "COMMAND_BLOCK_MINECART");
        // Improtant Reminder To Me
     /***
     *    If future 1.21.x patch renames something, add conditional mappings here.
     *     Example pattern:
     *    if (!v.isAtLeast(1, 21, 4)) { map.put("NEW_NAME", "OLD_NAME"); }
     *    Each one ofc depends on version gonna resolve this soon
     ***/

        return map;
    }
}
