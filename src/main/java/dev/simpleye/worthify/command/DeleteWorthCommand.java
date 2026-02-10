package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class DeleteWorthCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public DeleteWorthCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("worth.delete")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /deleteworth <player>");
            return true;
        }

        if (!plugin.getEconomyHook().isUsingInternalEconomy()) {
            sender.sendMessage(ChatColor.RED + "This command only works with Worthify internal economy.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        boolean ok = plugin.getEconomyHook().resetInternalBalance(target);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "Failed to reset balance.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Reset balance for " + ChatColor.WHITE + (target.getName() == null ? args[0] : target.getName()) + ChatColor.GREEN + ".");
        return true;
    }
}
