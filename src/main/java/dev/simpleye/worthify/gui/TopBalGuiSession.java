package dev.simpleye.worthify.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TopBalGuiSession {

    private static final Map<UUID, State> STATE = new ConcurrentHashMap<>();

    private TopBalGuiSession() {
    }

    public static void set(Player player, int page, int maxPages, int limit) {
        STATE.put(player.getUniqueId(), new State(page, maxPages, limit));
    }

    public static State get(Player player) {
        return STATE.get(player.getUniqueId());
    }

    public static void clear(Player player) {
        STATE.remove(player.getUniqueId());
    }

    public record State(int page, int maxPages, int limit) {
    }
}
