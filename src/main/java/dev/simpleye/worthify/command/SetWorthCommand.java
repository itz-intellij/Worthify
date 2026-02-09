package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!sender.hasPermission("worthify.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /setworth <number>");
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid number: " + args[0]);
            return true;
        }

        if (value < 0.0D) {
            sender.sendMessage(ChatColor.RED + "Worth must be >= 0");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "Hold an item in your main hand.");
            return true;
        }

        Material mat = hand.getType();
        boolean saved = plugin.getConfigManager().setPrice(mat, value);
        if (!saved) {
            sender.sendMessage(ChatColor.RED + "Failed to save prices.yml. Check console.");
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage(ChatColor.GREEN + "Set worth for " + mat.name() + " to " + value);
        return true;
    }
}
