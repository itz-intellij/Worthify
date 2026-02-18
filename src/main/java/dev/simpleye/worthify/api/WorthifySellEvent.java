package dev.simpleye.worthify.api;

import dev.simpleye.worthify.sell.SellResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class WorthifySellEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SellResult result;
    private final int unsellableCount;
    private final SellSource source;

    public WorthifySellEvent(Player player, SellResult result, int unsellableCount, SellSource source) {
        this.player = player;
        this.result = result;
        this.unsellableCount = unsellableCount;
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    public SellResult getResult() {
        return result;
    }

    public int getUnsellableCount() {
        return unsellableCount;
    }

    public SellSource getSource() {
        return source;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
