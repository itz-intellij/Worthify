package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.MultiplierGuiManager;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MultiplierCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;
    private final MultiplierGuiManager gui;

    public MultiplierCommand(WorthifyPlugin plugin, MultiplierGuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
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

        if (!plugin.isWorthMultiplierEnabled()) {
            sender.sendMessage(ChatColor.RED + "Worth multiplier is disabled in config.");
            return true;
        }

        gui.open(player);
        return true;
    }
}
