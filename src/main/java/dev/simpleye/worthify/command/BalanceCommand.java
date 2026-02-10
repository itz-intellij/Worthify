package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BalanceCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public BalanceCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!plugin.getEconomyHook().isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Economy is not available.");
            return true;
        }

        double balance = plugin.getEconomyHook().getBalance(player);
        sender.sendMessage(ChatColor.GREEN + "Balance: " + ChatColor.AQUA + "$" + SellService.formatMoney(balance));
        return true;
    }
}
