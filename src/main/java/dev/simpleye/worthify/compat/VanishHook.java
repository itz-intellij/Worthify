package dev.simpleye.worthify.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class VanishHook {

    private static volatile Method isInvisibleMethod;
    private static volatile boolean attempted;

    private VanishHook() {
    }

    public static boolean isVanished(Player player) {
        if (player == null) {
            return false;
        }

        if (Bukkit.getPluginManager().getPlugin("SuperVanish") == null
                && Bukkit.getPluginManager().getPlugin("PremiumVanish") == null) {
            return false;
        }

        Method m = resolve();
        if (m == null) {
            return false;
        }

        try {
            Object result = m.invoke(null, player);
            return result instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Method resolve() {
        if (attempted) {
            return isInvisibleMethod;
        }

        attempted = true;
        try {
            Class<?> api = Class.forName("de.myzelyam.api.vanish.VanishAPI");
            isInvisibleMethod = api.getMethod("isInvisible", Player.class);
        } catch (Exception ignored) {
            isInvisibleMethod = null;
        }
        return isInvisibleMethod;
    }
}
