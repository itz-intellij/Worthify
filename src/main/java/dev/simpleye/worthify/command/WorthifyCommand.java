package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class WorthifyCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;

    public WorthifyCommand(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();

        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            if (messages != null) {
                messages.send(sender, "worthify.help");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Worthify Commands:");
                sender.sendMessage(ChatColor.GRAY + "/worthify help" + ChatColor.WHITE + " - Show this help");
                sender.sendMessage(ChatColor.GRAY + "/worthify version" + ChatColor.WHITE + " - Show plugin version");
                sender.sendMessage(ChatColor.GRAY + "/worthify reload" + ChatColor.WHITE + " - Reload configs");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            String version = plugin.getDescription().getVersion();
            if (messages != null) {
                messages.send(sender, "worthify.version", "version", version);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Worthify v" + version);
            }

            String author = plugin.getDescription().getAuthors().isEmpty() ? plugin.getDescription().getName() : String.join(", ", plugin.getDescription().getAuthors());
            String discord = readPluginYmlValue("discord");
            String github = readPluginYmlValue("github");
            String modrinth = readPluginYmlValue("modrinth");

            sender.sendMessage(ChatColor.GRAY + "Author: " + ChatColor.WHITE + author);
            if (discord != null && !discord.isBlank()) {
                sender.sendMessage(ChatColor.GRAY + "Discord: " + ChatColor.WHITE + discord);
            }
            if (github != null && !github.isBlank()) {
                sender.sendMessage(ChatColor.GRAY + "GitHub: " + ChatColor.WHITE + github);
            }
            if (modrinth != null && !modrinth.isBlank()) {
                sender.sendMessage(ChatColor.GRAY + "Modrinth: " + ChatColor.WHITE + modrinth);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            if (messages != null) {
                messages.send(sender, "worthify.reloaded");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Worthify reloaded.");
            }
            return true;
        }

        if (messages != null) {
            messages.send(sender, "worthify.usage");
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /worthify reload");
        }
        return true;
    }

    private String readPluginYmlValue(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try (InputStream in = plugin.getResource("plugin.yml")) {
            if (in == null) {
                return null;
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            return cfg.getString(key, null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
