package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
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
        MessageService messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            if (messages != null) {
                messages.send(sender, "errors.players_only");
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        }

        if (args.length != 2) {
            if (messages != null) {
                messages.send(sender, "pay.usage");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            if (messages != null) {
                messages.send(sender, "pay.player_not_found", "player", args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found (must be online): " + args[0]);
            }
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            if (messages != null) {
                messages.send(sender, "pay.cannot_pay_self");
            } else {
                sender.sendMessage(ChatColor.RED + "You cannot pay yourself.");
            }
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            if (messages != null) {
                messages.send(sender, "pay.invalid_amount", "amount", args[1]);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            }
            return true;
        }

        if (amount <= 0.0D) {
            if (messages != null) {
                messages.send(sender, "pay.amount_must_be_positive");
            } else {
                sender.sendMessage(ChatColor.RED + "Amount must be > 0");
            }
            return true;
        }

        if (!plugin.getEconomyHook().isEnabled()) {
            if (messages != null) {
                messages.send(sender, "errors.economy_unavailable");
            } else {
                sender.sendMessage(ChatColor.RED + "Economy is not available.");
            }
            return true;
        }

        OfflinePlayer from = player;
        OfflinePlayer to = target;

        double balance = plugin.getEconomyHook().getBalance(from);
        if (balance < amount) {
            if (messages != null) {
                messages.send(sender, "pay.not_enough", "balance", SellService.formatMoney(balance));
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have enough money. Balance: $" + SellService.formatMoney(balance));
            }
            return true;
        }

        boolean ok = plugin.getEconomyHook().transfer(from, to, amount);
        if (!ok) {
            if (messages != null) {
                messages.send(sender, "pay.failed");
            } else {
                sender.sendMessage(ChatColor.RED + "Payment failed.");
            }
            return true;
        }

        if (messages != null) {
            messages.send(sender, "pay.sent", "player", target.getName(), "amount", SellService.formatMoney(amount));
            messages.send(target, "pay.received", "player", player.getName(), "amount", SellService.formatMoney(amount));
        } else {
            sender.sendMessage(ChatColor.GREEN + "You paid " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + " $" + SellService.formatMoney(amount) + ".");
            target.sendMessage(ChatColor.GREEN + "You received $" + SellService.formatMoney(amount) + " from " + ChatColor.WHITE + player.getName() + ChatColor.GREEN + ".");
        }
        return true;
    }
}
