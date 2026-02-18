package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.SellGuiHolder;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.api.SellSource;
import dev.simpleye.worthify.sell.SellProcessResult;
import dev.simpleye.worthify.sell.SellResult;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SellOnCloseGuiListener implements Listener {

    private final WorthifyPlugin plugin;
    private final SellService sellService;

    public SellOnCloseGuiListener(WorthifyPlugin plugin, SellService sellService) {
        this.plugin = plugin;
        this.sellService = sellService;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof SellGuiHolder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        SellProcessResult process = sellService.sellAllFromInventory(player, inv, SellSource.GUI_CLOSE);
        SellResult result = process.result();

        for (ItemStack item : process.unsellable()) {
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack stack : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
            }
        }

        MessageService messages = plugin.getMessages();
        if (result.economyMissing()) {
            if (messages != null) {
                messages.send(player, "errors.economy_unavailable");
            } else {
                player.sendMessage(ChatColor.RED + "Economy is not available.");
            }
            return;
        }

        if (!result.success()) {
            if (!process.unsellable().isEmpty()) {
                if (messages != null) {
                    messages.send(player, "sell.gui.unsellable_returned");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Some items were not sellable and were returned.");
                }
            }
            return;
        }

        if (messages != null) {
            messages.send(player, "sell.sold",
                    "amount", Integer.toString(result.soldAmount()),
                    "total", SellService.formatMoney(result.total()));
        } else {
            player.sendMessage(ChatColor.GREEN + "Sold " + result.soldAmount() + " items for $" + SellService.formatMoney(result.total()) + ".");
        }
        if (!process.unsellable().isEmpty()) {
            if (messages != null) {
                messages.send(player, "sell.gui.unsellable_returned");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Some items were not sellable and were returned.");
            }
        }
    }
}
