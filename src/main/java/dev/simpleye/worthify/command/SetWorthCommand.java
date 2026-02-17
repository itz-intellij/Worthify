package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SetWorthCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public SetWorthCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            if (messages != null) {
                messages.send(sender, "errors.players_only");
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        }

        if (!sender.hasPermission("worthify.admin") && !sender.isOp()) {
            if (messages != null) {
                messages.send(sender, "errors.no_permission");
            } else {
                sender.sendMessage(ChatColor.RED + "No permission.");
            }
            return true;
        }

        if (args.length != 1) {
            if (messages != null) {
                messages.send(sender, "setworth.usage");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /setworth <number>");
            }
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            if (messages != null) {
                messages.send(sender, "setworth.invalid_number", "number", args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid number: " + args[0]);
            }
            return true;
        }

        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            if (messages != null) {
                messages.send(sender, "setworth.invalid_value");
            } else {
                sender.sendMessage(ChatColor.RED + "Worth must be >= 0");
            }
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            if (messages != null) {
                messages.send(sender, "setworth.hold_item");
            } else {
                sender.sendMessage(ChatColor.RED + "Hold an item in your main hand.");
            }
            return true;
        }

        Material mat = hand.getType();
        boolean saved = plugin.getConfigManager().setPrice(mat, value);
        if (!saved) {
            if (messages != null) {
                messages.send(sender, "setworth.save_failed");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to save prices.yml. Check console.");
            }
            return true;
        }

        plugin.reloadPlugin();
        if (messages != null) {
            messages.send(sender, "setworth.set", "item", mat.name(), "value", Double.toString(value));
        } else {
            sender.sendMessage(ChatColor.GREEN + "Set worth for " + mat.name() + " to " + value);
        }
        return true;
    }
}
