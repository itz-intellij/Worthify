package dev.simpleye.worthify.listener;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.api.SellSource;
import dev.simpleye.worthify.sell.SellProcessResult;
import dev.simpleye.worthify.sell.SellResult;
import dev.simpleye.worthify.sell.SellService;
import dev.simpleye.worthify.selltools.SellToolType;
import dev.simpleye.worthify.selltools.SellToolUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SellToolListener implements Listener {

    private final WorthifyPlugin plugin;
    private final SellService sellService;
    private final SellToolUtil util;

    public SellToolListener(WorthifyPlugin plugin, SellService sellService, SellToolUtil util) {
        this.plugin = plugin;
        this.sellService = sellService;
        this.util = util;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = event.getItem();
        if (!util.isTool(hand, SellToolType.WAND)) {
            return;
        }

        if (!util.isEnabled(SellToolType.WAND)) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        if (!util.isAllowedContainer(clicked.getType())) {
            return;
        }

        if (!util.isActiveInRegistry(hand)) {
            handleExpired(player, hand);
            event.setCancelled(true);
            return;
        }

        if (util.isExpired(hand)) {
            handleExpired(player, hand);
            event.setCancelled(true);
            return;
        }

        boolean ok = sellContainer(player, clicked, util.shouldDestroyContainer(SellToolType.WAND));
        if (ok) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!util.isTool(tool, SellToolType.AXE)) {
            return;
        }

        if (!util.isEnabled(SellToolType.AXE)) {
            return;
        }

        Block block = event.getBlock();
        if (block == null) {
            return;
        }

        if (!util.isAllowedContainer(block.getType())) {
            return;
        }

        if (!util.isActiveInRegistry(tool)) {
            handleExpired(player, tool);
            event.setCancelled(true);
            return;
        }

        if (util.isExpired(tool)) {
            handleExpired(player, tool);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        sellContainer(player, block, util.shouldDestroyContainer(SellToolType.AXE));
    }

    private boolean sellContainer(Player player, Block block, boolean destroyContainer) {
        if (player == null || block == null) {
            return false;
        }

        BlockState state = block.getState();
        if (!(state instanceof Container container)) {
            return false;
        }

        Inventory inv = container.getInventory();
        if (inv == null) {
            return false;
        }

        SellSource source = destroyContainer ? SellSource.TOOL_AXE : SellSource.TOOL_WAND;
        SellProcessResult process = sellService.sellAllFromInventory(player, inv, source);
        SellResult result = process.result();

        MessageService messages = plugin.getMessages();

        if (result.economyMissing()) {
            if (messages != null) {
                messages.send(player, "errors.economy_unavailable");
            } else {
                player.sendMessage(ChatColor.RED + "Economy is not available.");
            }
            return true;
        }

        if (!result.success()) {
            if (messages != null) {
                messages.send(player, "sell.nothing_to_sell");
            } else {
                player.sendMessage(ChatColor.RED + "Nothing to sell.");
            }
            // Put unsellables back (sellAllFromInventory cleared inventory)
            for (ItemStack item : process.unsellable()) {
                inv.addItem(item);
            }
            return true;
        }

        if (messages != null) {
            messages.send(player, "sell.sold",
                    "amount", Integer.toString(result.soldAmount()),
                    "total", SellService.formatMoney(result.total()));
        } else {
            player.sendMessage(ChatColor.GREEN + "Sold " + result.soldAmount() + " items for $" + SellService.formatMoney(result.total()) + ".");
        }

        if (!process.unsellable().isEmpty()) {
            for (ItemStack item : process.unsellable()) {
                inv.addItem(item);
            }
            if (messages != null) {
                messages.send(player, "sell.gui.unsellable_returned");
            }
        }

        if (destroyContainer) {
            destroyBlock(block);
        }

        return true;
    }

    private void destroyBlock(Block block) {
        if (block == null) {
            return;
        }

        // Handle double chests: remove both halves
        BlockData data = block.getBlockData();
        if (data instanceof Chest chestData && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)) {
            if (chestData.getType() != Chest.Type.SINGLE) {
                Block other = getOtherChestHalf(block, chestData);
                if (other != null && other.getType() == block.getType()) {
                    other.setType(Material.AIR, false);
                }
            }
        }

        block.setType(Material.AIR, false);
    }

    private static Block getOtherChestHalf(Block block, Chest data) {
        if (block == null || data == null) {
            return null;
        }

        // In Bukkit, LEFT/RIGHT are relative to chest facing.
        org.bukkit.block.BlockFace facing = data.getFacing();
        org.bukkit.block.BlockFace left = rotateLeft(facing);
        org.bukkit.block.BlockFace right = rotateRight(facing);

        if (data.getType() == Chest.Type.LEFT) {
            return block.getRelative(right);
        }
        if (data.getType() == Chest.Type.RIGHT) {
            return block.getRelative(left);
        }
        return null;
    }

    private static org.bukkit.block.BlockFace rotateLeft(org.bukkit.block.BlockFace face) {
        return switch (face) {
            case NORTH -> org.bukkit.block.BlockFace.WEST;
            case WEST -> org.bukkit.block.BlockFace.SOUTH;
            case SOUTH -> org.bukkit.block.BlockFace.EAST;
            case EAST -> org.bukkit.block.BlockFace.NORTH;
            default -> org.bukkit.block.BlockFace.NORTH;
        };
    }

    private static org.bukkit.block.BlockFace rotateRight(org.bukkit.block.BlockFace face) {
        return switch (face) {
            case NORTH -> org.bukkit.block.BlockFace.EAST;
            case EAST -> org.bukkit.block.BlockFace.SOUTH;
            case SOUTH -> org.bukkit.block.BlockFace.WEST;
            case WEST -> org.bukkit.block.BlockFace.NORTH;
            default -> org.bukkit.block.BlockFace.NORTH;
        };
    }

    private void handleExpired(Player player, ItemStack item) {
        if (player == null || item == null) {
            return;
        }

        MessageService messages = plugin.getMessages();
        util.revoke(item);
        item.setAmount(0);
        if (messages != null) {
            messages.send(player, "selltools.expired");
        } else {
            player.sendMessage(ChatColor.RED + "This sell tool has expired.");
        }
    }
}
