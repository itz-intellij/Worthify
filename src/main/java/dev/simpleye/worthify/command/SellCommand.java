package dev.simpleye.worthify.command;

import dev.simpleye.worthify.gui.SellOnCloseGuiManager;
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

    public SellCommand(SellService sellService, SellOnCloseGuiManager sellGuiManager) {
        this.sellService = sellService;
        this.sellGuiManager = sellGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            sellGuiManager.open(player);
            return true;
        }

        String mode = args[0].toLowerCase();
        SellResult result;

        switch (mode) {
            case "hand" -> result = sellService.sellHand(player);
            case "all" -> result = sellService.sellAll(player);
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /sell [hand|all]");
                return true;
            }
        }

        if (result.economyMissing()) {
            sender.sendMessage(ChatColor.RED + "Economy is not available.");
            return true;
        }

        if (!result.success()) {
            sender.sendMessage(ChatColor.RED + "Nothing to sell.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Sold " + result.soldAmount() + " items for $" + SellService.formatMoney(result.total()) + ".");
        return true;
    }
}
