package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class WorthifyCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public WorthifyCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "Worthify reloaded.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /worthify reload");
        return true;
    }
}
