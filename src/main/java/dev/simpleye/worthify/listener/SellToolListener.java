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
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();
        Action action = event.getAction();

        // WAND click (right or left)
        if (util.isTool(hand, SellToolType.WAND) && util.isEnabled(SellToolType.WAND)) {
            String activation = util.getActivation(SellToolType.WAND);
            if (("RIGHT_CLICK_BLOCK".equalsIgnoreCase(activation) && action != Action.RIGHT_CLICK_BLOCK)
                    || ("LEFT_CLICK_BLOCK".equalsIgnoreCase(activation) && action != Action.LEFT_CLICK_BLOCK)) {
                return;
            }

            Block clicked = event.getClickedBlock();
            if (clicked == null || !util.isAllowedContainer(clicked.getType())) {
                return;
            }

            if (!util.isActiveInRegistry(hand)) {
                handleRevoked(player, hand);
                event.setCancelled(true);
                return;
            }

            if (util.isExpired(hand)) {
                handleExpired(player, hand, SellToolType.WAND);
                event.setCancelled(true);
                return;
            }

            if (!util.hasUses(hand)) {
                handleNoUses(player, hand, SellToolType.WAND);
                event.setCancelled(true);
                return;
            }

            SellResult result = sellContainer(player, clicked, util.shouldDestroyContainer(SellToolType.WAND));
            if (result != null) {
                if (result.success()) {
                    util.recordSellUse(hand, player.getName(), result.soldAmount(), result.total());
                    util.decrementUses(hand);
                    if (!util.hasUses(hand)) {
                        handleNoUses(player, hand, SellToolType.WAND);
                    }
                }
                event.setCancelled(true);
            }
            return;
        }

        // AXE click mode (optional)
        if (util.isTool(hand, SellToolType.AXE) && util.isEnabled(SellToolType.AXE)) {
            String activation = util.getActivation(SellToolType.AXE);
            if (!("RIGHT_CLICK_BLOCK".equalsIgnoreCase(activation) || "LEFT_CLICK_BLOCK".equalsIgnoreCase(activation))) {
                return;
            }
            if (("RIGHT_CLICK_BLOCK".equalsIgnoreCase(activation) && action != Action.RIGHT_CLICK_BLOCK)
                    || ("LEFT_CLICK_BLOCK".equalsIgnoreCase(activation) && action != Action.LEFT_CLICK_BLOCK)) {
                return;
            }

            Block clicked = event.getClickedBlock();
            if (clicked == null || !util.isAllowedContainer(clicked.getType())) {
                return;
            }

            if (!util.isActiveInRegistry(hand)) {
                handleRevoked(player, hand);
                event.setCancelled(true);
                return;
            }

            if (util.isExpired(hand)) {
                handleExpired(player, hand, SellToolType.AXE);
                event.setCancelled(true);
                return;
            }

            SellResult result = sellContainer(player, clicked, util.shouldDestroyContainer(SellToolType.AXE));
            if (result != null) {
                event.setCancelled(true);
            }
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

        if (!"BREAK".equalsIgnoreCase(util.getActivation(SellToolType.AXE))) {
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
            handleRevoked(player, tool);
            event.setCancelled(true);
            return;
        }

        if (util.isExpired(tool)) {
            handleExpired(player, tool, SellToolType.AXE);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        sellContainer(player, block, util.shouldDestroyContainer(SellToolType.AXE));
    }

    private SellResult sellContainer(Player player, Block block, boolean destroyContainer) {
        if (player == null || block == null) {
            return null;
        }

        BlockState state = block.getState();
        if (!(state instanceof Container container)) {
            return null;
        }

        Inventory inv = container.getInventory();
        if (inv == null) {
            return null;
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
            return result;
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
            return result;
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

        return result;
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

    private void handleExpired(Player player, ItemStack item, SellToolType type) {
        if (player == null || item == null || type == null) {
            return;
        }

        MessageService messages = plugin.getMessages();
        if (util.shouldSelfDestructOnExpiry(type)) {
            util.revoke(item);
            item.setAmount(0);
        }
        if (messages != null) {
            messages.send(player, "selltools.expired");
        } else {
            player.sendMessage(ChatColor.RED + "This sell tool has expired.");
        }
    }

    private void handleRevoked(Player player, ItemStack item) {
        if (player == null || item == null) {
            return;
        }
        util.revoke(item);
        item.setAmount(0);
        MessageService messages = plugin.getMessages();
        if (messages != null) {
            messages.send(player, "selltools.expired");
        } else {
            player.sendMessage(ChatColor.RED + "This sell tool has expired.");
        }
    }

    private void handleNoUses(Player player, ItemStack item) {
        handleNoUses(player, item, SellToolType.WAND);
    }

    private void handleNoUses(Player player, ItemStack item, SellToolType type) {
        if (player == null || item == null || type == null) {
            return;
        }

        util.revoke(item);
        item.setAmount(0);

        MessageService messages = plugin.getMessages();
        if (messages != null) {
            messages.send(player, "selltools.no_uses");
        } else {
            player.sendMessage(ChatColor.RED + "This sell tool has no uses left.");
        }
    }
}
