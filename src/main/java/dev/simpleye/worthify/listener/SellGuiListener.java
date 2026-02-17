package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.SellGuiHolder;
import dev.simpleye.worthify.gui.SellGuiManager;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.sell.SellResult;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;

public final class SellGuiListener implements Listener {

    private final WorthifyPlugin plugin;
    private final SellService sellService;

    public SellGuiListener(WorthifyPlugin plugin, SellService sellService) {
        this.plugin = plugin;
        this.sellService = sellService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGuiHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        List<Integer> collectSlots = cfg.getIntegerList("gui.sell.collect_slots");

        int rawSlot = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (rawSlot < top.getSize()) {
            if (rawSlot == SellGuiManager.SELL_BUTTON_SLOT || rawSlot == SellGuiManager.CLOSE_BUTTON_SLOT) {
                event.setCancelled(true);
            } else if (!collectSlots.contains(rawSlot)) {
                event.setCancelled(true);
            }
        } else {
            if (event.isShiftClick() && event.getClickedInventory() instanceof PlayerInventory) {
                ItemStack current = event.getCurrentItem();
                if (current != null && !current.getType().isAir()) {
                    int dest = firstEmpty(top, collectSlots);
                    if (dest != -1) {
                        event.setCancelled(true);
                        top.setItem(dest, current);
                        event.setCurrentItem(null);
                        player.updateInventory();
                    }
                }
            }
        }

        if (rawSlot == SellGuiManager.SELL_BUTTON_SLOT) {
            SellResult result = sellService.sellFromInventorySlots(player, event.getInventory(), collectSlots);

            MessageService messages = plugin.getMessages();

            if (result.economyMissing()) {
                if (messages != null) {
                    messages.send(player, "errors.economy_unavailable");
                } else {
                    player.sendMessage(ChatColor.RED + "Economy is not available.");
                }
            } else if (!result.success()) {
                if (messages != null) {
                    messages.send(player, "sell.nothing_to_sell");
                } else {
                    player.sendMessage(ChatColor.RED + "Nothing to sell.");
                }
            } else {
                if (messages != null) {
                    messages.send(player, "sell.sold",
                            "amount", Integer.toString(result.soldAmount()),
                            "total", SellService.formatMoney(result.total()));
                } else {
                    player.sendMessage(ChatColor.GREEN + "Sold " + result.soldAmount() + " items for $" + SellService.formatMoney(result.total()) + ".");
                }
            }

            return;
        }

        if (rawSlot == SellGuiManager.CLOSE_BUTTON_SLOT) {
            event.setCancelled(true);
            player.closeInventory();
        }
    }

    private static int firstEmpty(Inventory inventory, List<Integer> slots) {
        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                return slot;
            }
        }
        return -1;
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

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        List<Integer> collectSlots = cfg.getIntegerList("gui.sell.collect_slots");

        for (int slot : collectSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }

            inv.setItem(slot, null);
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack stack : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
            }
        }
    }
}
