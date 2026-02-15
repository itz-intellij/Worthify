package dev.simpleye.worthify.compat;

import org.bukkit.Material;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MaterialResolver {

    private final ServerVersion serverVersion;
    private final Map<String, String> aliases;
    private final Set<String> knownButUnsupported;
    private final Method matchMaterialLegacy;

    public MaterialResolver(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
        this.aliases = buildAliases(serverVersion);
        this.knownButUnsupported = buildKnownButUnsupported(serverVersion);
        this.matchMaterialLegacy = findMatchMaterialLegacy();
    }

    public Material resolve(String input) {
        if (input == null) {
            return null;
        }

        String key = normalize(input);
        if (key.isEmpty()) {
            return null;
        }

        Material direct = matchMaterialCompat(key);
        if (direct != null) {
            return direct;
        }

        String aliased = aliases.get(key);
        if (aliased != null) {
            return matchMaterialCompat(aliased);
        }

        return null;
    }

    public boolean isKnownButUnsupported(String input) {
        if (input == null) {
            return false;
        }
        return knownButUnsupported.contains(normalize(input));
    }

    public String getAliasTarget(String input) {
        if (input == null) {
            return null;
        }
        return aliases.get(normalize(input));
    }

    private static String normalize(String s) {
        String next = s.trim().toUpperCase(Locale.ROOT);
        if (next.startsWith("MINECRAFT:")) {
            next = next.substring("MINECRAFT:".length());
        }
        next = next.replace(' ', '_').replace('-', '_');
        return next;
    }

    private Material matchMaterialCompat(String key) {
        Material direct = Material.matchMaterial(key);
        if (direct != null) {
            return direct;
        }

        if (matchMaterialLegacy != null) {
            try {
                Object result = matchMaterialLegacy.invoke(null, key, true);
                if (result instanceof Material m) {
                    return m;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return null;
    }

    private static Method findMatchMaterialLegacy() {
        try {
            return Material.class.getMethod("matchMaterial", String.class, boolean.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
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

        map.put("BOOK_AND_QUILL", "WRITABLE_BOOK");
        map.put("BOTTLE_O_ENCHANTING", "EXPERIENCE_BOTTLE");
        map.put("REDSTONE_COMPARATOR", "COMPARATOR");
        map.put("REDSTONE_REPEATER", "REPEATER");
        map.put("STEAK", "COOKED_BEEF");
        map.put("RAW_RABBIT", "RABBIT");
        map.put("TURTLE_SHELL", "TURTLE_HELMET");

        map.put("LAPIS_LAZULI_BLOCK", "LAPIS_BLOCK");
        map.put("LAPIS_LAZULI_ORE", "LAPIS_ORE");
        map.put("DEEPSLATE_LAPIS_LAZULI_ORE", "DEEPSLATE_LAPIS_ORE");

        map.put("JIGSAW_BLOCK", "JIGSAW");
        map.put("FROGLIGHT", "OCHRE_FROGLIGHT");

        map.put("SCUTE", "TURTLE_SCUTE");
        map.put("ENDER_DRAGON_BANNER_PATTERN", "DRAGON_BANNER_PATTERN");
        map.put("SKELETON_BANNER_PATTERN", "SKULL_BANNER_PATTERN");
        map.put("THING_BANNER_PATTERN", "MOJANG_BANNER_PATTERN");

        map.put("AWKWARD_POTION", "POTION");
        map.put("AWKWARD_SPLASH_POTION", "SPLASH_POTION");
        map.put("AWKWARD_LINGERING_POTION", "LINGERING_POTION");
        map.put("THICK_POTION", "POTION");
        map.put("THICK_SPLASH_POTION", "SPLASH_POTION");
        map.put("THICK_LINGERING_POTION", "LINGERING_POTION");
        map.put("WATER_BOTTLE", "POTION");
        map.put("SPLASH_WATER_BOTTLE", "SPLASH_POTION");
        map.put("LINGERING_WATER_BOTTLE", "LINGERING_POTION");

        map.put("ARROW_OF_SPLASHING", "TIPPED_ARROW");

        map.put("UNWAXED_BLOCK_OF_COPPER", "COPPER_BLOCK");
        map.put("WAXED_BLOCK_OF_COPPER", "WAXED_COPPER_BLOCK");
        map.put("UNWAXED_CHISELED_COPPER", "CHISELED_COPPER");
        map.put("UNWAXED_COPPER_BULB", "COPPER_BULB");
        map.put("UNWAXED_COPPER_DOOR", "COPPER_DOOR");
        map.put("UNWAXED_COPPER_GRATE", "COPPER_GRATE");
        map.put("UNWAXED_COPPER_TRAPDOOR", "COPPER_TRAPDOOR");
        map.put("UNWAXED_CUT_COPPER", "CUT_COPPER");
        map.put("UNWAXED_CUT_COPPER_SLAB", "CUT_COPPER_SLAB");
        map.put("UNWAXED_CUT_COPPER_STAIRS", "CUT_COPPER_STAIRS");

        map.put("UNWAXED_EXPOSED_CHISELED_COPPER", "EXPOSED_CHISELED_COPPER");
        map.put("UNWAXED_EXPOSED_COPPER", "EXPOSED_COPPER");
        map.put("UNWAXED_EXPOSED_COPPER_BULB", "EXPOSED_COPPER_BULB");
        map.put("UNWAXED_EXPOSED_COPPER_DOOR", "EXPOSED_COPPER_DOOR");
        map.put("UNWAXED_EXPOSED_COPPER_GRATE", "EXPOSED_COPPER_GRATE");
        map.put("UNWAXED_EXPOSED_COPPER_TRAPDOOR", "EXPOSED_COPPER_TRAPDOOR");
        map.put("UNWAXED_EXPOSED_CUT_COPPER", "EXPOSED_CUT_COPPER");
        map.put("UNWAXED_EXPOSED_CUT_COPPER_SLAB", "EXPOSED_CUT_COPPER_SLAB");
        map.put("UNWAXED_EXPOSED_CUT_COPPER_STAIRS", "EXPOSED_CUT_COPPER_STAIRS");

        map.put("UNWAXED_OXIDIZED_CHISELED_COPPER", "OXIDIZED_CHISELED_COPPER");
        map.put("UNWAXED_OXIDIZED_COPPER", "OXIDIZED_COPPER");
        map.put("UNWAXED_OXIDIZED_COPPER_BULB", "OXIDIZED_COPPER_BULB");
        map.put("UNWAXED_OXIDIZED_COPPER_DOOR", "OXIDIZED_COPPER_DOOR");
        map.put("UNWAXED_OXIDIZED_COPPER_GRATE", "OXIDIZED_COPPER_GRATE");
        map.put("UNWAXED_OXIDIZED_COPPER_TRAPDOOR", "OXIDIZED_COPPER_TRAPDOOR");
        map.put("UNWAXED_OXIDIZED_CUT_COPPER", "OXIDIZED_CUT_COPPER");
        map.put("UNWAXED_OXIDIZED_CUT_COPPER_SLAB", "OXIDIZED_CUT_COPPER_SLAB");
        map.put("UNWAXED_OXIDIZED_CUT_COPPER_STAIRS", "OXIDIZED_CUT_COPPER_STAIRS");

        map.put("UNWAXED_WEATHERED_CHISELED_COPPER", "WEATHERED_CHISELED_COPPER");
        map.put("UNWAXED_WEATHERED_COPPER", "WEATHERED_COPPER");
        map.put("UNWAXED_WEATHERED_COPPER_BULB", "WEATHERED_COPPER_BULB");
        map.put("UNWAXED_WEATHERED_COPPER_DOOR", "WEATHERED_COPPER_DOOR");
        map.put("UNWAXED_WEATHERED_COPPER_GRATE", "WEATHERED_COPPER_GRATE");
        map.put("UNWAXED_WEATHERED_COPPER_TRAPDOOR", "WEATHERED_COPPER_TRAPDOOR");
        map.put("UNWAXED_WEATHERED_CUT_COPPER", "WEATHERED_CUT_COPPER");
        map.put("UNWAXED_WEATHERED_CUT_COPPER_SLAB", "WEATHERED_CUT_COPPER_SLAB");
        map.put("UNWAXED_WEATHERED_CUT_COPPER_STAIRS", "WEATHERED_CUT_COPPER_STAIRS");
        // Improtant Reminder To Me
     /***
     *    If future 1.21.x patch renames something, add conditional mappings here.
     *     Example pattern:
     *    if (!v.isAtLeast(1, 21, 4)) { map.put("NEW_NAME", "OLD_NAME"); }
     *    Each one ofc depends on version gonna resolve this soon
     ***/

        return map;
    }

    private static Set<String> buildKnownButUnsupported(ServerVersion v) {
        if (v == null) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        set.add("CHAIN");
        set.add("ENDER_DRAGON_BANNER_PATTERN");
        set.add("OMINOUS_BANNER");
        set.add("POLISHED_RESIN_BRICKS");
        set.add("SCUTE");
        set.add("SKELETON_BANNER_PATTERN");
        set.add("THING_BANNER_PATTERN");
        set.add("HARNESS");
        set.add("SHELF");
        set.add("WILD_FLOWERS");
        return set;
    }
}
