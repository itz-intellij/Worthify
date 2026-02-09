package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PayCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public PayCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found (must be online): " + args[0]);
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You cannot pay yourself.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }

        if (amount <= 0.0D) {
            sender.sendMessage(ChatColor.RED + "Amount must be > 0");
            return true;
        }

        if (!plugin.getEconomyHook().isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Economy is not available.");
            return true;
        }

        OfflinePlayer from = player;
        OfflinePlayer to = target;

        double balance = plugin.getEconomyHook().getBalance(from);
        if (balance < amount) {
            sender.sendMessage(ChatColor.RED + "You don't have enough money. Balance: $" + SellService.formatMoney(balance));
            return true;
        }

        boolean ok = plugin.getEconomyHook().transfer(from, to, amount);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "Payment failed.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "You paid " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + " $" + SellService.formatMoney(amount) + ".");
        target.sendMessage(ChatColor.GREEN + "You received $" + SellService.formatMoney(amount) + " from " + ChatColor.WHITE + player.getName() + ChatColor.GREEN + ".");
        return true;
    }
}
