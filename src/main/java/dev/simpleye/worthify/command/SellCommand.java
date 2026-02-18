package dev.simpleye.worthify.command;

import dev.simpleye.worthify.gui.SellOnCloseGuiManager;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.api.SellSource;
import dev.simpleye.worthify.sell.SellResult;
import dev.simpleye.worthify.sell.SellService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SellCommand implements CommandExecutor {

    private final SellService sellService;
    private final SellOnCloseGuiManager sellGuiManager;
    private final MessageService messages;

    public SellCommand(SellService sellService, SellOnCloseGuiManager sellGuiManager, MessageService messages) {
        this.sellService = sellService;
        this.sellGuiManager = sellGuiManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (messages != null) {
                messages.send(sender, "errors.players_only");
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        }

        if (args.length == 0) {
            sellGuiManager.open(player);
            return true;
        }

        String mode = args[0].toLowerCase();
        SellResult result;

        switch (mode) {
            case "hand" -> result = sellService.sellHand(player, SellSource.COMMAND_HAND);
            case "all" -> result = sellService.sellAll(player, SellSource.COMMAND_ALL);
            default -> {
                if (messages != null) {
                    messages.send(sender, "sell.usage");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /sell [hand|all]");
                }
                return true;
            }
        }

        if (result.economyMissing()) {
            if (messages != null) {
                messages.send(sender, "errors.economy_unavailable");
            } else {
                sender.sendMessage(ChatColor.RED + "Economy is not available.");
            }
            return true;
        }

        if (!result.success()) {
            if (messages != null) {
                messages.send(sender, "sell.nothing_to_sell");
            } else {
                sender.sendMessage(ChatColor.RED + "Nothing to sell.");
            }
            return true;
        }

        if (messages != null) {
            messages.send(sender, "sell.sold",
                    "amount", Integer.toString(result.soldAmount()),
                    "total", SellService.formatMoney(result.total()));
        } else {
            sender.sendMessage(ChatColor.GREEN + "Sold " + result.soldAmount() + " items for $" + SellService.formatMoney(result.total()) + ".");
        }
        return true;
    }
}
