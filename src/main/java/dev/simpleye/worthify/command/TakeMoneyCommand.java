package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.util.MoneyUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class TakeMoneyCommand implements CommandExecutor {

    public static final String PERMISSION = "worthify.takemoney";

    private final WorthifyPlugin plugin;

    public TakeMoneyCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();

        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission("worthify.admin") && !sender.isOp()) {
            if (messages != null && !messages.get("errors.no_permission").isEmpty()) {
                messages.send(sender, "errors.no_permission");
            } else {
                sender.sendMessage(ChatColor.RED + "No permission.");
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

        if (args.length != 2) {
            if (messages != null && !messages.get("takemoney.usage").isEmpty()) {
                messages.send(sender, "takemoney.usage");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /takemoney <player> <amount>");
            }
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            if (messages != null && !messages.get("takemoney.player_not_found").isEmpty()) {
                messages.send(sender, "takemoney.player_not_found", "player", args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            }
            return true;
        }

        Double parsed = MoneyUtil.parseAmount(args[1]);
        if (parsed == null) {
            if (messages != null && !messages.get("takemoney.invalid_amount").isEmpty()) {
                messages.send(sender, "takemoney.invalid_amount", "amount", args[1]);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            }
            return true;
        }

        double amount = parsed;
        if (amount <= 0.0D) {
            if (messages != null && !messages.get("takemoney.amount_must_be_positive").isEmpty()) {
                messages.send(sender, "takemoney.amount_must_be_positive");
            } else {
                sender.sendMessage(ChatColor.RED + "Amount must be > 0");
            }
            return true;
        }

        double balance = plugin.getEconomyHook().getBalance(target);
        if (balance < amount) {
            amount = balance;
        }

        if (amount <= 0.0D) {
            String name = target.getName() == null ? args[0] : target.getName();
            if (messages != null && !messages.get("takemoney.nothing_to_take").isEmpty()) {
                messages.send(sender, "takemoney.nothing_to_take", "player", name);
            } else {
                sender.sendMessage(ChatColor.RED + "Nothing to take from " + name + ".");
            }
            return true;
        }

        boolean ok = plugin.getEconomyHook().withdraw(target, amount);
        if (!ok) {
            if (messages != null && !messages.get("takemoney.failed").isEmpty()) {
                messages.send(sender, "takemoney.failed", "player", (target.getName() == null ? args[0] : target.getName()));
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to take money.");
            }
            return true;
        }

        String name = target.getName() == null ? args[0] : target.getName();
        if (messages != null && !messages.get("takemoney.taken").isEmpty()) {
            messages.send(sender, "takemoney.taken", "player", name, "amount", MoneyUtil.format(amount));
        } else {
            sender.sendMessage(ChatColor.GREEN + "Took $" + MoneyUtil.format(amount) + " from " + ChatColor.WHITE + name + ChatColor.GREEN + ".");
        }

        return true;
    }
}
